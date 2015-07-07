//////////////////////////////////////////////////////////////////////////////
//
//  Module:     replheap.dll
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//
//  Microsoft Research Detours Package, Version 2.1 (Build_207)
//
//

#define WIN32_LEAN_AND_MEAN
#ifndef COMPILING_ROCKALL_LIBRARY
#define COMPILING_ROCKALL_LIBRARY
#endif

#include <stdio.h>
#include <windows.h>
#include <assert.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#include <Dbghelp.h>
#include "detours.h"

#define DETOURED_CRT "msvcrt.dll"

#pragma comment(lib, "detours.lib") // include the library

#pragma comment(lib, "Dbghelp.lib") // include the library

#pragma comment(lib, "Kernel32.lib") // include the library

// Need to link with Ws2_32.lib
#pragma comment (lib, "Ws2_32.lib")

#define DEFAULT_BUFLEN 512
#define DEFAULT_PORT "7777"

static VOID * (CDECL * TrueMalloc)(size_t sz) = NULL;
static VOID (CDECL * TrueFree)(VOID * ptr) = NULL;
static VOID * (CDECL * TrueRealloc)(VOID *, size_t sz) = NULL;
static VOID * (CDECL * TrueCalloc)(size_t, size_t sz) = NULL;

SOCKET ClientSocket = INVALID_SOCKET;

bool SetupSocket(){

	SOCKET ListenSocket = INVALID_SOCKET;
	WSADATA wsaData;
    int iResult;

	struct addrinfo *result = NULL;
    struct addrinfo hints;

    int iSendResult;
    
    // Initialize Winsock
    iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
    if (iResult != 0) {
        printf("WSAStartup failed with error: %d\n", iResult);
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
        printf("getaddrinfo failed with error: %d\n", iResult);
        WSACleanup();
        return 1;
    }

    // Create a SOCKET for connecting to server
    ListenSocket = socket(result->ai_family, result->ai_socktype, result->ai_protocol);
    if (ListenSocket == INVALID_SOCKET) {
        printf("socket failed with error: %ld\n", WSAGetLastError());
        freeaddrinfo(result);
        WSACleanup();
        return 1;
    }

    // Setup the TCP listening socket
    iResult = bind( ListenSocket, result->ai_addr, (int)result->ai_addrlen);
    if (iResult == SOCKET_ERROR) {
        printf("bind failed with error: %d\n", WSAGetLastError());
        freeaddrinfo(result);
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    freeaddrinfo(result);

    iResult = listen(ListenSocket, SOMAXCONN);
    if (iResult == SOCKET_ERROR) {
        printf("listen failed with error: %d\n", WSAGetLastError());
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    // Accept a client socket
    ClientSocket = accept(ListenSocket, NULL, NULL);
    if (ClientSocket == INVALID_SOCKET) {
        printf("accept failed with error: %d\n", WSAGetLastError());
        closesocket(ListenSocket);
        WSACleanup();
        return 1;
    }

    // No longer need server socket
    closesocket(ListenSocket);
	

}

void printStack( void )
{
     unsigned int   i;
     void         * stack[ 100 ];
     unsigned short frames;
     SYMBOL_INFO  * symbol;
     HANDLE         process;

     process = GetCurrentProcess();
     SymInitialize( process, NULL, TRUE );
     frames               = CaptureStackBackTrace( 0, 100, stack, NULL );
     symbol               = ( SYMBOL_INFO * )TrueCalloc( sizeof( SYMBOL_INFO ) + 256 * sizeof( char ), 1 );
     symbol->MaxNameLen   = 255;
     symbol->SizeOfStruct = sizeof( SYMBOL_INFO );

     for( i = 0; i < frames; i++ ){
         SymFromAddr( process, ( DWORD64 )( stack[ i ] ), 0, symbol );
         printf( "%i: %s - 0x%0X\n", frames - i - 1, symbol->Name, symbol->Address );
     }

     TrueFree( symbol );
}


VOID * CDECL LTMalloc(size_t sz)
{
	return TrueMalloc(sz);
}

VOID CDECL LTFree(VOID * ptr)
{
	return TrueFree(ptr);
}

// XXX: optimize this
VOID * CDECL LTRealloc(VOID * ptr, size_t sz) {
	
	
	return TrueRealloc(ptr, sz);
}

VOID * CDECL LTCalloc( size_t Number, size_t Size ) {
	//int * c = (int *)AppHeap.New(Number*Size,NULL,true);
	//return (PVOID)c;
	return TrueCalloc( Number, Size ); 
}

void SetupHooks()
{
    // We couldn't call LoadLibrary in DllMain,
    // so we detour malloc/free/realloc/calloc here...
    LONG error;
	
    TrueMalloc = (VOID * (CDECL * )(size_t sz))
        DetourFindFunction(DETOURED_CRT, "malloc");
	TrueFree = (VOID (CDECL * )(VOID *))
        DetourFindFunction(DETOURED_CRT, "free");
	TrueRealloc = (VOID * (CDECL * )(VOID *, size_t sz))
        DetourFindFunction(DETOURED_CRT, "realloc");
	TrueCalloc = (VOID * (CDECL * )(size_t, size_t sz))
        DetourFindFunction(DETOURED_CRT, "calloc");

	printf("got malloc at 0x%x\n",TrueMalloc);
	printf("got free at 0x%x\n",TrueFree);
	printf("got realloc at 0x%x\n",TrueRealloc);
	printf("got calloc at 0x%x\n",TrueCalloc);

	void * foo = TrueMalloc(32);
	TrueFree(foo);
	printf("sampled CRT heap object: 0x%x\n",foo);

    DetourTransactionBegin();
    DetourUpdateThread(GetCurrentThread());

    DetourAttach(&(PVOID&)TrueMalloc,  LTMalloc);
	DetourAttach(&(PVOID&)TrueFree,    LTFree);
	DetourAttach(&(PVOID&)TrueRealloc, LTRealloc);
	DetourAttach(&(PVOID&)TrueCalloc,  LTCalloc);

    error = DetourTransactionCommit();

    if (error == NO_ERROR) {
        printf("replheap.dll: Detoured malloc functions.\n");
    }
    else {
        printf("replheap.dll: Error detouring malloc functions: %d\n", error);
    }

}

BOOL WINAPI DllMain(HINSTANCE hinst, DWORD dwReason, LPVOID reserved)
{
    LONG error;
    (void)hinst;
    (void)reserved;

    if (dwReason == DLL_PROCESS_ATTACH) {
        printf("replheap.dll: Starting.\n");
		fflush(stdout);

		SetupHooks();

		
    }
    else if (dwReason == DLL_PROCESS_DETACH) {

        DetourTransactionBegin();
        DetourUpdateThread(GetCurrentThread());
        DetourDetach(&(PVOID&)TrueMalloc, LTMalloc);
		DetourDetach(&(PVOID&)TrueFree, LTFree);
		DetourDetach(&(PVOID&)TrueRealloc, LTRealloc);
		DetourDetach(&(PVOID&)TrueCalloc, LTCalloc);
        error = DetourTransactionCommit();
		

        printf("simple.dll: Removed malloc() (result=%d)s.\n",
               error);
        fflush(stdout);
    }
    return TRUE;
}
