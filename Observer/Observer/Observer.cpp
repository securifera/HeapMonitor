#include <stdio.h>
#include <iomanip>
#include <winsock2.h>
#include <ws2tcpip.h>

#include "Observer.h"
#include "MinHook.h"
#include "dbghelp.h"


// Need to link with Ws2_32.lib
#pragma comment (lib, "Ws2_32.lib")

#define DEFAULT_MSGBUF_SIZE 50000
#define DEFAULT_BUFLEN 512
#define DEFAULT_PORT "7777"
SOCKET ClientSocket = INVALID_SOCKET;

typedef void * (__cdecl *PtrMalloc)(size_t);
typedef void (__cdecl *PtrFree)(void *);

// Hook tables. (Lot's of static data, but it's the only way to do this.)
const int numHooks = 128;

int nUsedMallocHooks = 0; 
int nUsedFreeHooks = 0; 

PtrMalloc mallocHooks[numHooks];
PtrFree freeHooks[numHooks];

PtrMalloc originalMallocs[numHooks];
PtrFree originalFrees[numHooks];

//global buffer
char ret_buf[DEFAULT_MSGBUF_SIZE];

HANDLE ghWriteEvent;
bool shouldHook = true;
bool profiling = true;

//Log File
FILE * pFile;

//Current module name
char moduleFileName[MAX_PATH];

CRITICAL_SECTION cs;  // shared structure


// Malloc hook function. Templated so we can hook many mallocs.
template <int N>
void * __cdecl mallocHook(size_t size){
	
	void * p = originalMallocs[N](size);
	if( profiling && shouldHook ){
		shouldHook = false;
	
		if(pFile)
			fprintf(pFile, "Hooked malloc for size %d at address %p.\n", size, p);

		//Create trace and malloc message, then add to queue
		Trace aTrace;
		MallocMessage aMsg(size, (size_t *)p, &aTrace);
		DWORD retBytes = 0;


		EnterCriticalSection( &cs );
		//Get buf data
		retBytes = aMsg.getBytes( ret_buf, DEFAULT_MSGBUF_SIZE );
		if( retBytes > 0 && ClientSocket ){
			
			//Send the bytes 
			int sent_count = send(ClientSocket, (const char *)ret_buf, retBytes, 0 );
			if(pFile)
				fprintf(pFile, "Sent message.  %d bytes, msg_size %d.\n", sent_count, retBytes);
			
		}
		LeaveCriticalSection( &cs );
		shouldHook = true;
	} 

	return p;
}

// Free hook function.
template <int N>
void  __cdecl freeHook(void * p){

	originalFrees[N](p);
	if( profiling && shouldHook ){
		shouldHook = false;

		if(pFile)
			fprintf(pFile, "Hooked free of pointer %p\n", p);
		
		Trace aTrace;
		FreeMessage aMsg( (size_t *)p, &aTrace);
        DWORD retBytes = 0;

		EnterCriticalSection( &cs );
		//Get buf data
		retBytes = aMsg.getBytes( ret_buf, DEFAULT_MSGBUF_SIZE );
		if( retBytes > 0 && ClientSocket ){
			
			//Send the bytes 
			send(ClientSocket, (const char *)ret_buf, retBytes, 0 );
			if(pFile)
				fprintf(pFile, "Send message.  %d bytes\n", retBytes);
			
		}
		LeaveCriticalSection( &cs );
		shouldHook = true;
	} 

}

// Template recursion to init a hook table.
template<int N> struct InitNHooks{
    static void initHook(){
        InitNHooks<N-1>::initHook();  // Compile time recursion. 

		mallocHooks[N-1] = &mallocHook<N-1>;
		freeHooks[N-1] = &freeHook<N-1>;
    }
};
 
template<> struct InitNHooks<0>{
    static void initHook(){
		// stop the recursion
    }
};

bool SetupSocket(){

	SOCKET ListenSocket = INVALID_SOCKET;
	WSADATA wsaData;
    int iResult;

	struct addrinfo *result = NULL;
    struct addrinfo hints;

    //int iSendResult;
    
    // Initialize Winsock
    iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        if(pFile)
			fprintf(pFile, "WSAStartup failed with error: %d\n", iResult);
        return 1;
    }

    ZeroMemory(&hints, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    hints.ai_flags = AI_PASSIVE;

    // Resolve the server address and port
    iResult = getaddrinfo(NULL, DEFAULT_PORT, &hints, &result);
    if ( iResult != 0 ) {
        if(pFile)
			fprintf(pFile, "getaddrinfo failed with error: %d\n", iResult);
        WSACleanup();
        return 1;
    }

    // Create a SOCKET for connecting to server
    ListenSocket = socket(result->ai_family, result->ai_socktype, result->ai_protocol);
    if (ListenSocket == INVALID_SOCKET) {
        if(pFile)
			fprintf(pFile, "socket failed with error: %ld\n", WSAGetLastError());
        freeaddrinfo(result);
        WSACleanup();
        return 1;
    }

    // Setup the TCP listening socket
    iResult = bind( ListenSocket, result->ai_addr, (int)result->ai_addrlen);
    if (iResult == SOCKET_ERROR) {
        if(pFile)
			fprintf(pFile, "bind failed with error: %d\n", WSAGetLastError());
        freeaddrinfo(result);
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    freeaddrinfo(result);

    iResult = listen(ListenSocket, SOMAXCONN);
    if (iResult == SOCKET_ERROR) {
        if(pFile)
			fprintf(pFile, "listen failed with error: %d\n", WSAGetLastError());
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    // Accept a client socket
	struct sockaddr_in client_info = {0}; 
    ClientSocket = accept(ListenSocket, (sockaddr*)&client_info, NULL);
    if (ClientSocket == INVALID_SOCKET) {
        if(pFile)
			fprintf(pFile, "accept failed with error: %d\n", WSAGetLastError());
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

	char *connected_ip = inet_ntoa(client_info.sin_addr);
    int port = ntohs(client_info.sin_port); 

    if(pFile)
		fprintf(pFile, "-[IP: %s, Connected on PORT:%d]\n", connected_ip, port ); 


    // No longer need server socket
    closesocket(ListenSocket);

	return 0;
}

// Callback which recieves addresses for mallocs/frees which we hook.
BOOL CALLBACK enumSymbolsCallback(PSYMBOL_INFO symbolInfo, ULONG symbolSize, PVOID userContext){
	
	//Causes deadlock
	//std::lock_guard<std::mutex> lk(hookTableMutex);

	PCSTR moduleName = (PCSTR)userContext;
	
	// Hook mallocs.
	if(strcmp(symbolInfo->Name, "malloc") == 0){
		if(nUsedMallocHooks >= numHooks){
			if(pFile)
				fprintf(pFile, "All malloc hooks used up!\n");
			return true;
		}
		if(pFile)
			fprintf(pFile, "Hooking malloc from module %s into malloc hook num %d.\n", moduleName, nUsedMallocHooks);
		if(MH_CreateHook((void*)symbolInfo->Address, mallocHooks[nUsedMallocHooks],  (void **)&originalMallocs[nUsedMallocHooks]) != MH_OK){
			if(pFile)
				fprintf(pFile, "Create hook malloc failed!\n");
		}

		if(MH_EnableHook((void*)symbolInfo->Address) != MH_OK){
			if(pFile)
				fprintf(pFile, "Enable malloc hook failed!\n");
		}

		nUsedMallocHooks++;
	}

	// Hook frees.
	if(strcmp(symbolInfo->Name, "free") == 0){
		if(nUsedFreeHooks >= numHooks){
			if(pFile)
				fprintf(pFile, "All free hooks used up!\n");
			return true;
		}
		if(pFile)
			fprintf(pFile, "Hooking free from module %s into free hook num %d.\n", moduleName, nUsedFreeHooks);
		if(MH_CreateHook((void*)symbolInfo->Address, freeHooks[nUsedFreeHooks],  (void **)&originalFrees[nUsedFreeHooks]) != MH_OK){
			if(pFile)
				fprintf(pFile, "Create hook free failed!\n");
		}

		if(MH_EnableHook((void*)symbolInfo->Address) != MH_OK){
			if(pFile)
				fprintf(pFile, "Enable free failed!\n");
		}

		nUsedFreeHooks++;
	}

	return true;
}

BOOL CALLBACK EnumerateModulesProc64Callback( PCSTR  ModuleName, DWORD64 BaseOfDll, PVOID UserContext ){
	
	// TODO: Hooking msvcrt causes problems with cleaning up stdio - avoid for now.
	if(strcmp(ModuleName, "msvcrt") == 0) 
		return true;

	if(pFile)
		fprintf(pFile, "Found module %s\n", ModuleName);

	//Do not hook the current module mallocs/frees
	if( strstr(moduleFileName, ModuleName ) != NULL )
		return true;

	SymEnumSymbols(GetCurrentProcess(), BaseOfDll, "malloc", enumSymbolsCallback, (void*)ModuleName);
	SymEnumSymbols(GetCurrentProcess(), BaseOfDll, "free", enumSymbolsCallback, (void*)ModuleName);
	return true;

}

void setupHeapProfiling(){

	InitializeCriticalSection(&cs);

	//Open log file
	fopen_s(&pFile, "C:\\hooklog.txt","w");

	//Accept connection
	if( SetupSocket() ){
		if(pFile)
			fclose(pFile);
		return;
	}
	
	// We use printfs thoughout injection becasue it's just safer/less troublesome
	// than iostreams for this sort of low-level/hacky/threaded work.
	if(pFile)
		fprintf(pFile, "Injecting library...\n");

	nUsedMallocHooks = 0;
	nUsedFreeHooks = 0;
	
	// Create our hook pointer tables using template meta programming fu.
	InitNHooks<numHooks>::initHook(); 

	// Init min hook framework.
	MH_STATUS ret = MH_Initialize(); 
	if( ret != MH_OK ){
		if(pFile)
			fprintf(pFile, "MH_Init failed\n");
		return;
	}

	// Init dbghelp framework.
	if(!SymInitialize(GetCurrentProcess(), NULL, true)){
		if(pFile)
			fprintf(pFile, "SymInitialize failed\n");
        
		return;
	}

	//Used to get module name of current DLL not EXE
	HMODULE hMod = NULL;
	GetModuleHandleEx(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
          reinterpret_cast<LPCWSTR>(&setupHeapProfiling), &hMod );

	GetModuleFileNameA(hMod, moduleFileName, MAX_PATH);
	if(pFile)
		fprintf(pFile, "Module Filename: %s\n", moduleFileName) ;

	//// Yes this leaks - cleauing it up at application exit has zero real benefit.
	//// Might be able to clean it up on CatchExit but I don't see the point.
	////heapProfiler = new HeapProfiler(); 

	//// Trawl though loaded modules and hook any mallocs and frees we find.
	//SymEnumerateModules(GetCurrentProcess(), enumModulesCallback, NULL);
	SymEnumerateModules64(GetCurrentProcess(), EnumerateModulesProc64Callback, NULL);

	
}

extern "C"{

BOOL APIENTRY DllMain(HANDLE hModule, DWORD reasonForCall, LPVOID lpReserved){
	switch (reasonForCall){
		case DLL_PROCESS_ATTACH:
			setupHeapProfiling();
		break;
		case DLL_THREAD_ATTACH:
		break;
		case DLL_THREAD_DETACH:
		break;
		case DLL_PROCESS_DETACH:			
			if(pFile)
				fprintf(pFile, "Unhooking all\n");
			profiling = false;
			MH_Uninitialize();

			//Close file
			if(pFile)
				fclose(pFile);

			DeleteCriticalSection(&cs);
		break;
	}

	return TRUE;
}

}