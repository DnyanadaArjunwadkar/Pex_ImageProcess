package outco;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

/**
 * 
 * @author Dynanada Arjunwadkar (dnyanada@buffalo.edu)
 *
 */
public class ImageProcessPex {

	private static final String INPUT_FILE_PATH = "/Users/dny/Desktop/urls.txt";
	private static final String OUTPUT_FILE_PATH = "/Users/dny/Desktop/out.csv";
	private static final int IMAGE_SLICE_THREAD_COUNT = 1;
	private static final ConcurrentMap<ArrayList<Integer>, Integer> pixelMap = new ConcurrentHashMap<>();

	private static BufferedReader bufferedReader = null;
	private static PrintWriter printWriter = null;

	/**
	 * Code driver.
	 * 
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {

		// Code timing begins.
		long start = System.currentTimeMillis();

		try 
		{
			printWriter = new PrintWriter(new File(OUTPUT_FILE_PATH));
			bufferedReader = new BufferedReader(new FileReader(INPUT_FILE_PATH));

			String sCurrentLine;
			while ((sCurrentLine = bufferedReader.readLine()) != null) 
			{
				System.out.println(sCurrentLine);
				// Download Image.
				BufferedImage image = ImageIO.read(new URL(sCurrentLine));

				// Read image into a HashMap
				readImage(image);

				// Write to CSV
				writeToCsv(sCurrentLine);

				// Clear HashMap for next iteration/
				pixelMap.clear();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Silent closing
			try {
				if (bufferedReader != null)
					bufferedReader.close();
				printWriter.close();
				} 
			catch (IOException ex) 
				{
					 ex.printStackTrace();
				}
		}

		// Code timing ends.
		System.out.println("Time taken :" + (System.currentTimeMillis() - start));
	}// main

	/**
	 * This functions loads the image from given URL. 
	 * check RGB values of every pixel in the image.
	 * 
	 * 
	 * 
	 * @param image
	 * @throws InterruptedException
	 */
	private static void readImage(final BufferedImage image) throws InterruptedException {

		final int width = image.getWidth();
		final int height = image.getHeight();

		ExecutorService executorService = Executors.newFixedThreadPool(IMAGE_SLICE_THREAD_COUNT);

		int slice = height / IMAGE_SLICE_THREAD_COUNT;
		for (int n = 0; n < height; n = n + slice) {

			final int endRange = (n + slice > height) ? height : n + slice;
			final int startRange = n;

			executorService.submit(new Runnable() {
				@Override
				public void run() {
					for (int i = startRange; i < endRange; i++) {
						for (int j = 0; j < width; j++) {
							syncThis(j, i, image);
						}
					}
				}
			});
		}

		executorService.shutdown();

		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 *  using multiple threads, updates frequency of each pixel in the pixelMap.
	 * 
	 * @param xPixel
	 * @param yPixel
	 * @param image
	 */
	private static void syncThis(int xPixel, int yPixel, BufferedImage image) {

		final int pixel = image.getRGB(xPixel, yPixel);
		final ArrayList<Integer> al = pixelRGB(pixel);

		if (pixelMap.putIfAbsent(al, 1) != null) {
			pixelMap.put(al, pixelMap.get(al) + 1);
		}
	}

	/**
	 * returns the RGB value of each pixel, builds ArtayList object that acts as a Key in pixelMap
	 * 
	 * @param pixel
	 * @return
	 */
	public static ArrayList<Integer> pixelRGB(int pixel) {
		int red = (pixel >> 16) & 0xff;
		int green = (pixel >> 8) & 0xff;
		int blue = (pixel) & 0xff;
		ArrayList<Integer> al = new ArrayList<Integer>();
		al.add(red);
		al.add(green);
		al.add(blue);
		return al;
	}

	/**
	 * This function sorts the pixelMap based on frequencies of each colored pixel.
	 * find the 3 highest frequencies in the map and writes output to CSV file in the format:
	 * url;color;color;color;
	 * 
	 * @param sCurrentLine
	 */
	private static void writeToCsv(String sCurrentLine) {
		Map<ArrayList<Integer>, Integer> map = sortByValues(pixelMap);

		StringBuilder sb = new StringBuilder();
		sb.append(sCurrentLine);
		sb.append(",");
		System.out.println("urls is:" + sCurrentLine);

		List<Entry<ArrayList<Integer>, Integer>> entryList = new ArrayList<Map.Entry<ArrayList<Integer>, Integer>>(
				map.entrySet());

		ArrayList<Integer> al = null;
		Entry<ArrayList<Integer>, Integer> dominant1 = entryList.get(0);
		al = dominant1.getKey();

		sb.append("\"");
		sb.append(al);
		sb.append("\"");
		sb.append(',');
		al.clear();

		Entry<ArrayList<Integer>, Integer> dominant2 = entryList.get(1);
		al = dominant2.getKey();

		sb.append("\"");
		sb.append(al);
		sb.append("\"");
		sb.append(',');
		al.clear();

		Entry<ArrayList<Integer>, Integer> dominant3 = entryList.get(2);

		al = dominant3.getKey();
		sb.append("\"");
		sb.append(al);
		sb.append("\"");
		al.clear();

		sb.append('\n');
		System.out.println(sb.toString());
		printWriter.write(sb.toString());
	}

	/**
	 *following function is used to sort values of pixelMap in non decreasing order
	 * 
	 * @param hm2
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static HashMap<ArrayList<Integer>, Integer> sortByValues(ConcurrentMap<ArrayList<Integer>, Integer> hm2) {
		List<?> list = new LinkedList<Map.Entry<ArrayList<Integer>, Integer>>(hm2.entrySet());
		// Defined Custom Comparator here
		Collections.sort(list, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				return ((Comparable<Integer>) ((Map.Entry<ArrayList<Integer>, Integer>) (o2)).getValue())
						.compareTo(((Map.Entry<ArrayList<Integer>, Integer>) (o1)).getValue());
			}
		});

		HashMap<ArrayList<Integer>, Integer> sortedHashMap = new LinkedHashMap<ArrayList<Integer>, Integer>();
		for (Iterator<?> it = list.iterator(); it.hasNext();) {
			Map.Entry<ArrayList<Integer>, Integer> entry = (Map.Entry<ArrayList<Integer>, Integer>) it.next();
			sortedHashMap.put(entry.getKey(), entry.getValue());
		}
		return sortedHashMap;
	}
}// class ends
