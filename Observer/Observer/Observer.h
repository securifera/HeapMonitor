#pragma once

#include "dbghelp.h"
#include <iomanip>

const int backtraceSize = 64;

class Trace {

public:	
	Trace(HANDLE passedHeapHandle){
		heapHandle = passedHeapHandle;
	    trace_str_buf_size = 0;
		this->walkStack();
	};

	~Trace(){
		if( trace_str_buf != NULL && heapHandle != NULL )
			HeapFree(heapHandle, 0 , trace_str_buf);

	};
	char *trace_str_buf;
	DWORD trace_str_buf_size;
	HANDLE heapHandle;

private:

	void appendToTrace( char *str, size_t size ){

		if( heapHandle != NULL){
			if( trace_str_buf_size == 0 ){

				//Create a buffer and copy into it
				trace_str_buf_size = 0x100000;
				trace_str_buf = (char *)HeapAlloc(heapHandle, HEAP_ZERO_MEMORY, trace_str_buf_size);
				strncpy_s(trace_str_buf, trace_str_buf_size, str, size );

			} else if( strlen(trace_str_buf) + size > trace_str_buf_size) {
		
				//Add more space
				trace_str_buf_size += 0x100000;
				trace_str_buf = (char *)HeapReAlloc(heapHandle, HEAP_ZERO_MEMORY, trace_str_buf, trace_str_buf_size);
				if( trace_str_buf == NULL) 
					return;
				//Concatenate the string
				strncat_s(trace_str_buf, trace_str_buf_size, str, size);

			} else {

				//Concatenate the string
				strncat_s(trace_str_buf, trace_str_buf_size, str, size);
			}
		}
	
	}

	void walkStack(){
		
		CONTEXT Context = {0};
		STACKFRAME64 stk;
		memset(&stk, 0, sizeof(stk));

		HANDLE hThread = GetCurrentThread();
		HANDLE currProc = GetCurrentProcess();

		const int MAXSYMBOLNAME = 128 - sizeof(IMAGEHLP_SYMBOL);
		char symbol64_buf[sizeof(IMAGEHLP_SYMBOL) + MAXSYMBOLNAME] = {0};
		IMAGEHLP_SYMBOL *symbol = reinterpret_cast<IMAGEHLP_SYMBOL*>(symbol64_buf);
		symbol->SizeOfStruct = sizeof(IMAGEHLP_SYMBOL);
		symbol->MaxNameLength = MAXSYMBOLNAME - 1;

		DWORD IMG_ARCH = IMAGE_FILE_MACHINE_I386;
		RtlCaptureContext( &Context );
		#ifdef _WIN64
			IMG_ARCH = IMAGE_FILE_MACHINE_AMD64;
			stk.AddrPC.Offset       = Context.Rip;
			stk.AddrPC.Mode         = AddrModeFlat;
			stk.AddrStack.Offset    = Context.Rsp;
			stk.AddrStack.Mode      = AddrModeFlat;
			stk.AddrFrame.Offset    = Context.Rbp;
			stk.AddrFrame.Mode      = AddrModeFlat;
        #elif _WIN32
			stk.AddrPC.Offset       = Context.Eip;
			stk.AddrPC.Mode         = AddrModeFlat;
			stk.AddrStack.Offset    = Context.Esp;
			stk.AddrStack.Mode      = AddrModeFlat;
			stk.AddrFrame.Offset    = Context.Ebp;
			stk.AddrFrame.Mode      = AddrModeFlat;
		#endif

		char tmpBuf[24];
		char lineNum[20];
		DWORD str_len = 0;
		for(ULONG Frame = 0; ; Frame++)
		{
			BOOL result = StackWalk64(
									IMG_ARCH,   // __in      DWORD MachineType,
									currProc,        // __in      HANDLE hProcess,
									hThread,         // __in      HANDLE hThread,
									&stk,                       // __inout   LP STACKFRAME64 StackFrame,
									&Context,                  // __inout   PVOID ContextRecord,
									NULL,                     // __in_opt  PREAD_PROCESS_MEMORY_ROUTINE64 ReadMemoryRoutine,
									SymFunctionTableAccess64,                      // __in_opt  PFUNCTION_TABLE_ACCESS_ROUTINE64 FunctionTableAccessRoutine,
									SymGetModuleBase64,                     // __in_opt  PGET_MODULE_BASE_ROUTINE64 GetModuleBaseRoutine,
									NULL                       // __in_opt  PTRANSLATE_ADDRESS_ROUTINE64 TranslateAddress
									);

			size_t curr_trace = (ULONG64)stk.AddrPC.Offset;
			if( curr_trace ){

				sprintf_s(tmpBuf, "%p", curr_trace);
				// Output stack frame symbols if available.
				if(SymGetSymFromAddr(currProc, (DWORD64)curr_trace, 0, symbol)){

					appendToTrace(symbol->Name, strlen(symbol->Name));

					// Output filename + line info if available.
					IMAGEHLP_LINE64 lineSymbol;
					lineSymbol.SizeOfStruct = sizeof(IMAGEHLP_LINE64);
					DWORD displacement;

					if(SymGetLineFromAddr64(currProc, (DWORD64)curr_trace, &displacement, &lineSymbol)){
						
						appendToTrace( "\t", 1);
						appendToTrace( lineSymbol.FileName, strlen(lineSymbol.FileName));
						appendToTrace( ":", 1);
						
						//Add line number
						_ltoa_s(lineSymbol.LineNumber, lineNum, 10 );
						appendToTrace( lineNum, strlen(lineNum));

					} else {
						appendToTrace( "\t", 1);
					}
				
					appendToTrace( "\t", 1);

					appendToTrace( tmpBuf, strlen(tmpBuf));

					appendToTrace( ")\n", 2);

				} else {
					
					appendToTrace("<no symbol>\t\t(", 14);

					appendToTrace(tmpBuf, strlen(tmpBuf));

					appendToTrace( ")\n", 2);
				}
			
			}
			
			if(!result)
				break;
		}

	
	}
	
};

//******************************************************************************
class MemoryMessage{
public:
	MemoryMessage(size_t *ptr, Trace *trace ){
		this->ptr = ptr;
		this->trace = trace;
	};

	size_t getBytes(char* buf, size_t max_size){
		size_t ret_size = 0;
		size_t buf_size = 12;
		
		//Check that an address 
		if( trace ){

			DWORD trace_len = (DWORD)strlen(trace->trace_str_buf);
			buf_size += trace_len;	
			if( buf && buf_size <= max_size ){
				ret_size = buf_size;
				memset(buf, 0, ret_size );

				//Add address and stack trace
				*(long *)buf = (long)ptr;
				*(DWORD *)((char*)buf + 8) = (DWORD)trace_len;
				memcpy(  (char*)buf + 12, trace->trace_str_buf, trace_len );
			} else {
				printf("MemoryMessage: Unable to create MemoryMessage, buffer is too small.\nProvided: %d, Needed: %d\n", max_size, buf_size );
			}
		}
		return ret_size;

	}
	byte msg_type;
private:
	size_t *ptr;
	Trace *trace;
};

class MallocMessage : public MemoryMessage{
public:
	MallocMessage(DWORD size, size_t *ptr, Trace *trace ) : MemoryMessage( ptr, trace) {
		this->size = size;
		msg_type = (byte)0x12;
	};

	size_t getBytes(char* buf, DWORD max_size){
		size_t ret_size = 0;
		size_t buf_size = 0;
		
		//Get data from the base class
		buf_size = this->MemoryMessage::getBytes(buf + 5, max_size - 5);
		if( buf_size > 0 ){

			buf_size += 5;
			if( buf && buf_size <= max_size ){

				ret_size =  buf_size;
				//Add type
				*(byte *)buf = (byte)msg_type;	
				//Add size
				*((DWORD *)((char *)(buf + 1))) = size;
						
			} else {
				printf("MallocMessage: Unable to create MallocMessage, buffer is too small.\nProvided:%d, Needed:%d\n", max_size, buf_size );
			}
		}

		return ret_size;
	}
private:
	DWORD size;
};

class FreeMessage : public MemoryMessage{
public:
	FreeMessage( size_t *ptr, Trace *trace ) : MemoryMessage( ptr, trace) {
		msg_type = (byte)0x13;
	};

	size_t getBytes(char* buf, DWORD max_size){
		size_t ret_size = 0;
		size_t buf_size = 0;
		
		//Get data from the base class
		buf_size = this->MemoryMessage::getBytes(buf + 1, max_size - 1);

		if( buf && buf_size > 0 ){

			buf_size += 1;
			if( buf && buf_size <= max_size ){
				ret_size =  buf_size;

				//Add type
				*(byte *)buf = (byte)msg_type;	

			}  else {
				printf("FreeMessage: Unable to create FreeMessage, buffer is too small.\nProvided:%d, Needed:%d\n", max_size, buf_size );
			}
		}

		return ret_size;
	
	}
};