package com.pman.distributedurlshortener.server;

public class Base64Converter {

	static Character[] chars = new Character[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
			'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
			'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'x', 'y', 'z', '_', '-' };

	// shuffled array containing [0-9a-zA-Z-_]+
	private static final char[] input = new char[] { '-', 'c', 'h', 'i', 'j', '5', 'V', 'M', 'n', 'H', 'I', '3', 'u', 'U', 'G', 'b', 'F',
			't', 'v', 'L', 'E', '2', 'm', '8', 'k', 'g', 'a', 'R', '_', 'r', 'P', 'w', 'y', '9', 'W', 'S', 'z', 's',
			'6', 'T', 'q', '1', '4', 'K', '0', 'p', 'N', 'l', 'J', 'Z', 'B', '7', 'A', 'X', 'e', 'd', 'Q', 'O', 'C',
			'f', 'o', 'Y', 'x', 'D' };

	private static final int len = input.length;
	
	public static void main(String[] args) throws InterruptedException {
		System.out.println(input);
		
		Thread t1 = new Thread(new Runnable() {
			@Override
			public void run() {
				long start = 100000, end = 100100;
				while (start < end) {
					System.out.println(longToBase64(start++));
				}
			}
		});

		Thread t2 = new Thread(new Runnable() {
			@Override
			public void run() {
				long start = 0, end = 1000;
				while (start < end) {
					System.out.println(longToBase64(start++));
				}
			}
		});
		
		t1.start();
		t2.start();
		
		t1.join();
		t2.join();
		
	}
	
	private static String longToBase64(long num) {
		StringBuilder sb = new StringBuilder();
		do {
			sb.append(input[(int) (num % len)]);
			num /= len;
		} while (num % len > 0);
		return sb.reverse().toString();
	}
}
