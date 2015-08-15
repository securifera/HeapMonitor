#include <Windows.h>
#include <Psapi.h>
#include <strsafe.h>

#include <iostream>
#include <stdio.h>
#include <conio.h>
#include <thread>
#include <vector>
#include <string>
#include <algorithm>

typedef LONG (NTAPI *NtSuspendProcess)(IN HANDLE ProcessHandle);
typedef LONG (NTAPI *NtResumeProcess)(IN HANDLE ProcessHandle);
typedef NTSTATUS (NTAPI *NtRtlCreateUserThread)( IN HANDLE ProcessHandle,
  IN PSECURITY_DESCRIPTOR SecurityDescriptor OPTIONAL,
  IN BOOLEAN CreateSuspended,
  IN ULONG StackZeroBits,
  IN OUT PULONG StackReserved,
  IN OUT PULONG StackCommit,
  IN PVOID StartAddress,
  IN PVOID StartParameter OPTIONAL,
  OUT PHANDLE ThreadHandle,
  OUT VOID* ClientID
);

// Inject a DLL into the target process by creating a new thread at LoadLibrary
// Waits for injected thread to finish and returns its exit code.
// 
// Originally from :
// http://www.codeproject.com/Articles/2082/API-hooking-revealed 
int LoadLibraryInjection(HANDLE proc, const char *dllName){

	int retVal;
	LPVOID RemoteString, LoadLibAddy;
	LoadLibAddy = (LPVOID)GetProcAddress(GetModuleHandleA("kernel32.dll"), "LoadLibraryA");

	RemoteString = (LPVOID)VirtualAllocEx(proc, NULL, strlen(dllName), MEM_RESERVE|MEM_COMMIT, PAGE_READWRITE);
	if(RemoteString == NULL){
		CloseHandle(proc); // Close the process handle.
		throw std::runtime_error("LoadLibraryInjection: Error on VirtualAllocEx.");
	}

	if(WriteProcessMemory(proc, (LPVOID)RemoteString, dllName,strlen(dllName), NULL) == 0){
		VirtualFreeEx(proc, RemoteString, 0, MEM_RELEASE); // Free the memory we were going to use.
		CloseHandle(proc); // Close the process handle.
		throw std::runtime_error("LoadLibraryInjection: Error on WriteProcessMemeory.");
	}

	HANDLE hThread;
	NtRtlCreateUserThread pfnNtRtlCreateUserThread = (NtRtlCreateUserThread)GetProcAddress( GetModuleHandleA("ntdll"), "RtlCreateUserThread");
	NTSTATUS retNtVal = pfnNtRtlCreateUserThread(proc, NULL, FALSE, 0, NULL, NULL, (LPTHREAD_START_ROUTINE)LoadLibAddy, (LPVOID)RemoteString, &hThread, NULL);
	//if((hThread = CreateRemoteThread(proc, NULL, NULL, (LPTHREAD_START_ROUTINE)LoadLibAddy, (LPVOID)RemoteString, NULL, NULL)) == NULL){
	if( retNtVal ){
		VirtualFreeEx(proc, RemoteString, 0, MEM_RELEASE); // Free the memory we were going to use.
		CloseHandle(proc); // Close the process handle.
		throw std::runtime_error("LoadLibraryInjection: Error on CreateRemoteThread.");
	}

	// Wait for the thread to finish.
	WaitForSingleObject(hThread, INFINITE);

	// Lets see what it says...
	//DWORD dwThreadExitCode=0;
	GetExitCodeThread(hThread,  (LPDWORD)&retVal);

	// No need for this handle anymore, lets get rid of it.
	CloseHandle(hThread);

	// Lets clear up that memory we allocated earlier.
	VirtualFreeEx(proc, RemoteString, 0, MEM_RELEASE);

	return retVal;
}

std::string getDirectoryOfFile(const std::string &file){
	size_t pos = (std::min)(file.find_last_of("/"), file.find_last_of("\\"));
	if(pos == std::string::npos)
		return ".";
	else
		return file.substr(0, pos);
}


//Suspend the process
NTSTATUS suspend(HANDLE processHandle){

    NtSuspendProcess pfnNtSuspendProcess = (NtSuspendProcess)GetProcAddress( GetModuleHandleA("ntdll"), "NtSuspendProcess");
    return pfnNtSuspendProcess(processHandle);
}

//Resume the process
NTSTATUS resume(HANDLE processHandle){
	
    NtResumeProcess pfnNtResumeProcess = (NtResumeProcess)GetProcAddress( GetModuleHandleA("ntdll"), "NtResumeProcess");
    return pfnNtResumeProcess(processHandle);
}

//START OF CODE
int enableSEPrivilege(LPCTSTR name) 
{
	HANDLE hToken;
	LUID luid;
	TOKEN_PRIVILEGES tkp;

	if(!OpenProcessToken(GetCurrentProcess(), TOKEN_ALL_ACCESS, &hToken)) return 0;

    if(!LookupPrivilegeValue(NULL, name, &luid)) return 0;

	tkp.PrivilegeCount = 1;
	tkp.Privileges[0].Luid = luid;
	tkp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;

    if(!AdjustTokenPrivileges(hToken, false, &tkp, sizeof(tkp), NULL, NULL)) return 0;

	if(GetLastError() == ERROR_NOT_ALL_ASSIGNED) return 0;

	CloseHandle(hToken);
	return 1;
}


extern "C" int main(int argc, char* argv[]){
	
	
	if(argc < 2){
		std::cout << "No arguments specified!\n\n";
		std::cout << "Usage: Heapy -p pid -e <exe path> -a [args to pass to exe]\n\n"
					 "       -p Provide the process pid if you wish to attach to a running process\n"
		             "       -e Path to exe to launch \n"
					 "       -d Path to dll to inject \n"
		             "       -a Any arguments for the specified exe\n";
			 
		return -1;
	}

	//Loop through args and set flags
	char *curArg;
	char *injectionTarget = NULL;
	int processId = 0;
	std::string commandLine = "";
	std::string dllPath = "";

	for( int i =1; i < argc; i++){
		curArg = argv[i];	
		if( strcmp(curArg, "-p") == 0 ){

			//Get the process id
			if( argc > i + 1 ){
				char *procIdStr = argv[i+1];	
				processId = atoi(procIdStr);
			}

		} else if( strcmp(curArg, "-e")   == 0){
			//Get the exe path
			if( argc > i + 1){
				injectionTarget = argv[i+1];	
				commandLine = injectionTarget;
				i++;				
			}
	    } else if( strcmp(curArg, "-a")   == 0){
			
			//Loop through the args
			for(int j = i+1; j < argc; ++j){
				commandLine += " " + std::string(argv[j]);
			}			
			
		} else if( strcmp(curArg, "-d")   == 0){
			//Get the process id
			if( argc > i + 1){
				dllPath = argv[i+1];	
				i++;
			}
			break;
		}
	}

	//Make sure dll exists
	if ( GetFileAttributesA((LPCSTR)dllPath.c_str()) == INVALID_FILE_ATTRIBUTES ){
		std::cerr << "DLL does not exist. Please check path: (" << dllPath << ").\n\n";
		return -1;
	}

	// Start our new process with a suspended main thread.
	std::cout << "Starting process with heap profiling enabled..." << std::endl;

	//If process id was given
	HANDLE processHandle; 
	HANDLE threadHandle = 0;

	if( processId != 0 ){

		DWORD dwResult = enableSEPrivilege(SE_DEBUG_NAME);
		processHandle = OpenProcess(PROCESS_ALL_ACCESS, FALSE, processId );
		if( processHandle == NULL ){
			std::cerr << "Error attaching to process " << processId << std::endl;
			return -1;
		}

		std::cout << "Target process id: " << processId << std::endl;

		NTSTATUS retVal =  suspend(processHandle );
		if( retVal ){
			std::cerr << "Error suspending process " << processId << " Code: " << retVal << std::endl;
			return 1;
		}

	} else if( !commandLine.empty() ){

		// Start our new process with a suspended main thread.
		std::cout << "Target exe path: " << injectionTarget << std::endl;
		std::cout << "Target exe command line: " << commandLine << std::endl;
		

		DWORD flags = CREATE_SUSPENDED;
		PROCESS_INFORMATION pi;
		STARTUPINFOA si;
		GetStartupInfoA(&si);

		if(CreateProcessA(NULL, (LPSTR)commandLine.c_str(), NULL, NULL, 0, flags, NULL, 
						 (LPSTR)".", &si, &pi) == 0){
			int err = GetLastError();
			std::cerr << "Error creating process " << injectionTarget << " Code: " << err << std::endl;
			return -1;
		}

		processHandle = pi.hProcess; 
		threadHandle = pi.hThread;

	}

	try{

		int retVal = LoadLibraryInjection(processHandle, dllPath.c_str());
		if( retVal == 0 ){
			throw std::runtime_error("LoadLibrary failed!");
		}

	} catch(const std::exception &e){
		std::cerr << "\n";
		std::cerr << "Error while injecting process: " << e.what() << "\n\n";
		std::cerr << "Check that the hook dll (" << dllPath << " is in the correct location.\n\n";
	
		if( processId != 0 )
			resume(processHandle);

		// TODO: figure out how to terminate thread. This does not always work.
		return -1;
	}
	
	// Once the injection thread has returned it is safe to resume the main thread.
	if( threadHandle){
		ResumeThread(threadHandle);
		std::cout << "Resuming thread handle.\n" << std::endl;
	} else if( processId != 0 ){
		if( !resume(processHandle))
			std::cout << "Sucessfully resumed process.\n" << std::endl;
	}

	std::cout << "Sucessfully injected Hooking DLL.\n" << std::endl;
	
	return 0;
}
