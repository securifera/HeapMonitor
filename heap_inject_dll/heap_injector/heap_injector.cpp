// heap_injector.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

#undef UNICODE
#include <cstdio>
#include <windows.h>
#include <detours.h>

#pragma comment(lib, "detours.lib") // include the library

int main()
{
    STARTUPINFO si;
    PROCESS_INFORMATION pi;
    ZeroMemory(&si, sizeof(STARTUPINFO));
    ZeroMemory(&pi, sizeof(PROCESS_INFORMATION));
    si.cb = sizeof(STARTUPINFO);
    char* DirPath = new char[MAX_PATH];
    char* DLLPath = new char[MAX_PATH]; //testdll.dll

    GetCurrentDirectory(MAX_PATH, DirPath);
    sprintf_s(DLLPath, MAX_PATH, "%s\\heap_inject.dll", DirPath);

    DetourCreateProcessWithDll(NULL, "C:\\windows\\notepad.exe", NULL,
        NULL, FALSE, CREATE_DEFAULT_ERROR_MODE, NULL, NULL,
        &si, &pi, DLLPath, NULL);

    delete [] DirPath;
    delete [] DLLPath;

    return 0;
}


