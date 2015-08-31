#pragma once
#include <ostream>
#include "dbghelp.h"
#include <sstream> 
#include <string>
#include <iomanip>

const int backtraceSize = 64;

class Trace {

public:	
	Trace(){
		//this->getTrace();
		this->walkStack();
	};
	std::string trace_str;

private:
	void walkStack(){
		
		CONTEXT Context = {0};
		STACKFRAME64 stk;
		memset(&stk, 0, sizeof(stk));
		std::stringstream stream;
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
			sprintf_s(tmpBuf, "%p",  (ULONG64)stk.AddrPC.Offset);
			if( curr_trace ){

				sprintf_s(tmpBuf, "%p", curr_trace);
				// Output stack frame symbols if available.
				if(SymGetSymFromAddr(currProc, (DWORD64)curr_trace, 0, symbol)){

					stream << symbol->Name;

					// Output filename + line info if available.
					IMAGEHLP_LINE64 lineSymbol;
					lineSymbol.SizeOfStruct = sizeof(IMAGEHLP_LINE64);
					DWORD displacement;

					if(SymGetLineFromAddr64(currProc, (DWORD64)curr_trace, &displacement, &lineSymbol)){
						stream << "\t" << lineSymbol.FileName; 
						stream << ":";
						
						//Add line number
						_ltoa_s(lineSymbol.LineNumber, lineNum, 10 );
						stream << lineNum;

					} else {
						stream << "\t";
					}
				
					
					stream << "\t(";
					stream << tmpBuf;	
					stream <<  ")\n";

				} else {
					
					stream << "<no symbol>\t\t(";
					stream << tmpBuf;	
					stream <<  ")\n";
				}
			
			}
			
			if(!result)
				break;
		}

		//Set the stream
		trace_str.assign( stream.str() );
	
	}
	void getTrace(){

		std::stringstream stream;
		unsigned long retLong;
		void *backtrace[backtraceSize];
		unsigned short frames;
		char tmpBuf[24];
		char lineNum[20];

		frames = CaptureStackBackTrace(1, backtraceSize, backtrace, &retLong);

		HANDLE process = GetCurrentProcess();

		const int MAXSYMBOLNAME = 128 - sizeof(IMAGEHLP_SYMBOL);
		char symbol64_buf[sizeof(IMAGEHLP_SYMBOL) + MAXSYMBOLNAME] = {0};
		IMAGEHLP_SYMBOL *symbol = reinterpret_cast<IMAGEHLP_SYMBOL*>(symbol64_buf);
		symbol->SizeOfStruct = sizeof(IMAGEHLP_SYMBOL);
		symbol->MaxNameLength = MAXSYMBOLNAME - 1;
		
		// Print out stack trace. Skip the first frame (that's our hook function.)
		for(unsigned short i = 0; i < frames; ++i){ 

			size_t curr_trace = (size_t)backtrace[i];
			if( curr_trace ){

				sprintf_s(tmpBuf, "%p", curr_trace);
				// Output stack frame symbols if available.
				if(SymGetSymFromAddr(process, (DWORD64)curr_trace, 0, symbol)){

					stream << symbol->Name;

					// Output filename + line info if available.
					IMAGEHLP_LINE64 lineSymbol;
					lineSymbol.SizeOfStruct = sizeof(IMAGEHLP_LINE64);
					DWORD displacement;

					if(SymGetLineFromAddr64(process, (DWORD64)curr_trace, &displacement, &lineSymbol)){
						stream << "\t" << lineSymbol.FileName; 
						stream << ":";
						
						//Add line number
						_ltoa_s(lineSymbol.LineNumber, lineNum, 10 );
						stream << lineNum;

					} else {
						stream << "\t";
					}
				
					
					stream << "\t(";
					stream << tmpBuf;	
					stream <<  ")\n";

				} else {
					
					stream << "\t<no symbol>\t(";
					stream << tmpBuf;	
					stream <<  ")\n";
				}

			} else{
				break;
			}
		}

		//Set the stream
		trace_str.assign( stream.str() );
	}
};

//******************************************************************************
class MemoryMessage{
public:
	MemoryMessage(size_t *ptr, Trace *trace ){
		this->ptr = ptr;
		this->trace = trace;
	};
	DWORD getBytes(char* buf, DWORD max_size){
		DWORD ret_size = 0;
		DWORD buf_size = 12;
		
		//Check that an address 
		if( trace ){

			buf_size += trace->trace_str.length();	
			if( buf && buf_size <= max_size ){
				ret_size = buf_size;
				memset(buf, 0, ret_size );

				//Add address and stack trace
				*(long *)buf = (size_t)ptr;
				*(int *)((char*)buf + 8) = trace->trace_str.length();
				memcpy(  (char*)buf + 12, trace->trace_str.c_str(), trace->trace_str.length() );
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

	DWORD getBytes(char* buf, DWORD max_size){
		DWORD ret_size = 0;
		DWORD buf_size = 0;
		
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

	DWORD getBytes(char* buf, DWORD max_size){
		DWORD ret_size = 0;
		DWORD buf_size = 0;
		
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