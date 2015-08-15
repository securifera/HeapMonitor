// TestAllocations.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <stdlib.h>
#include <Windows.h>


int _tmain(int argc, _TCHAR* argv[])
{
	char *test;
	
	printf("Current pid: %d\n", GetCurrentProcessId());
    printf("Sleeping, attach profiler\n", GetCurrentProcessId());

	Sleep(10000);

	int count = 1000;
	for( int i = 0; i < count; i++ ){

		int u = ((int)rand() % 500) + 8;
		test = (char*) malloc(u);
		printf("Allcated %d bytes.\n",u);
		Sleep(2000);
		free(test);
		printf("Freed bytes.\n",u);
		Sleep(2000);
	}
	return 0;
}

