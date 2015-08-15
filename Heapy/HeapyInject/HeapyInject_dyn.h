#pragma once
#include <ostream>
#include "dbghelp.h"
#include <sstream> 
#include <string>
#include <iomanip>

const int backtraceSize = 100;

class Trace {

public:	
	Trace(){
		this->getTrace();
	};
	std::string trace_str;

private:
	void getTrace(){

		std::stringstream stream;
		unsigned long retLong;
		void *backtrace[backtraceSize];
		unsigned short frames;
		char tmpBuf[24];
		char lineNum[20];

		frames = CaptureStackBackTrace(0, backtraceSize, backtrace, &retLong);

		HANDLE process = GetCurrentProcess();

		const int MAXSYMBOLNAME = 128 - sizeof(IMAGEHLP_SYMBOL);
		char symbol64_buf[sizeof(IMAGEHLP_SYMBOL) + MAXSYMBOLNAME] = {0};
		IMAGEHLP_SYMBOL *symbol = reinterpret_cast<IMAGEHLP_SYMBOL*>(symbol64_buf);
		symbol->SizeOfStruct = sizeof(IMAGEHLP_SYMBOL);
		symbol->MaxNameLength = MAXSYMBOLNAME - 1;
		
		// Print out stack trace. Skip the first frame (that's our hook function.)
		for(size_t i = 1; i < frames; ++i){ 

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
				
					//stream << "\t(" << std::setw(sizeof(void*)*2) << std::setfill('0') << curr_trace 
					stream << "\t(";
					stream << tmpBuf;	
					stream <<  ")\n";

				} else {
					//stream << "\t<no symbol>" << "\t(" << std::setw(sizeof(void*)*2) << std::setfill('0') << curr_trace <<  ")\n";
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
	char* getBytes( DWORD *retBytes ){
		DWORD buf_size = 12;
		char *retPtr = NULL;
		
		//Check that an address 
		if( trace ){

			buf_size += trace->trace_str.length();	
			retPtr = (char *)malloc(buf_size);
			memset(retPtr, 0, buf_size );

			//Add address and stack trace
			*(long *)retPtr = (size_t)ptr;
			*(int *)((char*)retPtr + 8) = trace->trace_str.length();
			memcpy(  (char*)retPtr + 12, trace->trace_str.c_str(), trace->trace_str.length() );

		}

		*retBytes = buf_size;
		return retPtr;

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

	char* getBytes(  DWORD *retBytes ){
		DWORD ret_size = 0;
		DWORD child_size = 0;
		char *retPtr = NULL;
		char *basePtr = NULL;
		
		//Get data from the base class
		basePtr = this->MemoryMessage::getBytes( &child_size );
		if( basePtr && child_size > 0 ){

			ret_size = child_size + 5;
			retPtr = (char *)malloc( ret_size);
			memset(retPtr, 0, ret_size );

			//Add type
			*(byte *)retPtr = (byte)msg_type;	
			//Add size
			*((DWORD *)((char *)(retPtr + 1))) = size;
			//Add internal 
			memcpy(retPtr + 5, basePtr, child_size );
			//Free internal buffer
			free(basePtr);
		}

		*retBytes = ret_size;
		return retPtr;
	}
private:
	DWORD size;
};

class FreeMessage : public MemoryMessage{
public:
	FreeMessage( size_t *ptr, Trace *trace ) : MemoryMessage( ptr, trace) {
		msg_type = (byte)0x13;
	};

	char* getBytes(  DWORD *retBytes ){
		DWORD ret_size = 0;
		DWORD child_size = 0;
		char *retPtr = NULL;
		char *basePtr = NULL;
		
		//Get data from the base class
		basePtr = this->MemoryMessage::getBytes( &child_size );
		if( basePtr && child_size > 0 ){

			ret_size = child_size + 1;
			retPtr = (char *)malloc( ret_size);
			memset(retPtr, 0, ret_size );

			//Add type
			*(byte *)retPtr = (byte)msg_type;	
			//Add internal 
			memcpy(retPtr + 1, basePtr, child_size );
			//Free internal buffer
			free(basePtr);

		}

		*retBytes = ret_size;
		return retPtr;
	
	}
};