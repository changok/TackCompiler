import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

class Offset {
	int _offset; String _memory;
	Offset(int offset, String memory) {_offset = offset; _memory = memory;}
}

class CCStack {
	private Map<String, Offset> _stack_layout;
	
	int _offset;
	CCStack() {
		_stack_layout = new LinkedHashMap<String, Offset>();
		_offset = -8;
	}
	
	boolean containKey(String value) {
		return _stack_layout.containsKey(value);
	}
	void push(String name) {
		if (!containKey(name)) {
			_stack_layout.put(name, new Offset(_offset , "[%rbp" + _offset + "]"));
			_offset = _offset - 8;
		}
	}
	void push(Address addr) {
		if (!containKey(addr._name)) {
			_stack_layout.put(addr._name, new Offset(_offset , "[%rbp" + _offset + "]"));
			_offset = _offset - 8;
		}
	}
	int getOffset(String name) {
		return _stack_layout.get(name)._offset;
	}
	String getMemory(String name) {
		return _stack_layout.get(name)._memory;
	}
	  int getFrameSize() {
		  return -(_offset+8);
	  }
	void print() {
		System.out.println("Stack Layout -------> ");
		for (Iterator<String> it = _stack_layout.keySet().iterator(); it.hasNext() ;) {
			String name = it.next();
			System.out.println(_stack_layout.get(name)._memory + ":" + name);
		}
		System.out.println("--------------------> ");
	}
}