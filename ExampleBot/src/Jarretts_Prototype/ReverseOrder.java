package Jarretts_Prototype;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReverseOrder<T> implements Iterable<T>{

	//I wanted to make something to reverse the list of stuff to do
	//Used directly in base management order, maybe useful in other places?
	
	private final List<T> originalList;
	
	public ReverseOrder(List<T> originalList) {
		this.originalList = originalList;
	}

	//Return the reversed iterator
	@Override
	public Iterator<T> iterator() {
		final ListIterator<T> i = originalList.listIterator(originalList.size()); 
		
		return new Iterator<T>() {
			public boolean hasNext() { return i.hasPrevious(); }
			public T next() { return i.previous(); }
			public void remove() { i.remove();}
		};
	}
	
	//Static call used in programs
	public static <T> ReverseOrder<T> reversed(List<T> list)
	{
		return new ReverseOrder<T>(list);
	}
	

}
