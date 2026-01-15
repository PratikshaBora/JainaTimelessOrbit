package com.timelessOrbit.entity;

import java.util.ArrayList;
import java.util.List;

public class ListRemoveTest {

	public static void main(String[] args) {

		List<String> names = new ArrayList<String>();
		names.add("aaaa");
		names.add("bbbb");

		names.add("cccc");
		names.add("dddd");
		
		System.out.println(names.size());
		names.remove("cccc");
		System.out.println(names.size());
	}

}
