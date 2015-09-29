// TestAllocations.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include <stdlib.h>
#include <Windows.h>


int _tmain(int argc, _TCHAR* argv[])
{
	
	printf("Current pid: %d\n", GetCurrentProcessId());
    printf("Sleeping, attach profiler\n", GetCurrentProcessId());

	Sleep(5000);

	const int numAllocs = 10;
	char *smallmem[numAllocs];
	int size = 0xf0;
	for( int i = 0; i < 4; i++ ){

		smallmem[i] = (char*) malloc(size);
		printf("Allcated %d bytes.\n",size);
		Sleep(1000);
	}

	size = 0x178;
	for( int i = 4; i < 8; i++ ){

		smallmem[i] = (char*) malloc(size);
		printf("Allocated %d bytes.\n",size);
		Sleep(1000);
	}


	size = 812;
	char *mem[numAllocs];
	for( int i = 0; i < numAllocs; i++ ){

		mem[i] = (char*) malloc(size);
		printf("Allcated %d bytes.\n",size);
		Sleep(500);
	}

	//Make hole
	free(mem[4]);
	//free(mem[5]);

	/*for( int i = 0; i < numAllocs; i++ ){
		free(mem[i]);
		printf("Freed bytes.\n",size);
		Sleep(2000);
	}*/
		
	//Allocate
	mem[8] = (char*) malloc(0xf0);

	//Make second hole
	free(mem[7]);

	//Allocate
	mem[9] = (char*) malloc(0x178);

	/*for( int i = 0; i < numAllocs; i++ ){
		free(mem[i]);
		printf("Freed bytes.\n",size);
		Sleep(1000);
	}*/

	
	/*int count = 1000;
	for( int i = 0; i < count; i++ ){

		int u = ((int)rand() % 500) + 8;
		test = (char*) malloc(u);
		printf("Allcated %d bytes.\n",u);
		Sleep(2000);
		free(test);
		printf("Freed bytes.\n",u);
		Sleep(2000);
	}*/
	return 0;
}

