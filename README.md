Heap Monitor
=====

HeapMonitor is a tool to allow you to visually monitor the heap memory allocations and deallocations happening 
in a Windows application in real-time on a remote system. It also provides a stack trace of each allocation and 
free so you can locate where the calls are being made from.  

The tool is composed of two main parts.

HeapMonitor GUI
--------
This is the GUI that displays the allocations and frees occurring in the target application.

![ScreenShot](https://www.securifera.com/wp-content/uploads/2016/01/heap_tool.jpg)


Observer
--------
This is the native c++ code that is responsible for hooking the heap allocation function calls. It consists of the 
DllInjector and the Observer DLL. As can probably be presumed, the DllInjector application injects the Observer DLL
into the target process. Once the DLL has been injected, it waits for a connection on port 7777 from the HeapMonitor GUI. 

The Observer application uses the MinHook library (https://github.com/TsudaKageyu/minhook) for function hooking
and built upon Luke Dodd's Heapy tool (https://github.com/lukedodd/Heapy).
