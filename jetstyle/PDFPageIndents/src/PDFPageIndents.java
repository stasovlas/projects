import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

import org.json.*;

/**
 * @author stasovlas
 *
 */
public class PDFPageIndents {
		static String pdfPath = null;
		static String savingFolder = null;
		static File pdfFile = null;
		static JSONArray leftJson = new JSONArray();
		static JSONArray rightJson = new JSONArray();
		
	public static void main(String[] args) throws IOException{
		if(args.length == 0){
			System.out.println("Не указан путь к pdf-файлу!");
			System.exit(0);
		} else if(args.length == 1){
			pdfPath = args[0];
			if(!new File(pdfPath).getName().endsWith(".pdf")){
				System.out.println("Указан путь не к pdf-файлу!");
				System.exit(0);
			} 
			
		} else{
			pdfPath = args[0];
			if(!new File(pdfPath).getName().endsWith(".pdf")){
				System.out.println("Указан путь не к pdf-файлу!");
				System.exit(0);
			}
			savingFolder = args[1];
		} 
		
		PDFFile pdf = getPdfFile();
		for (int i = 1; i < pdf.getNumPages() + 1; i++){
			System.out.println("Обработка страницы " + i + " из " + pdf.getNumPages() + "...");
			BufferedImage pageAsImage = getPageAsImage(pdf.getPage(i));
            findPageIndents(pageAsImage, i);
            if(savingFolder != null) savePageAsImage(pageAsImage, i);
		}
		savePageSorting();
		System.out.println("Обработка завершена");
	}
	
	/**
	 * Считывание pdf-файла
	 * @return считанный pdf-файл
	 */
	private static PDFFile getPdfFile(){
		try {
			pdfFile = new File(pdfPath);
			System.out.println("Чтение " + pdfFile.getName() + "...");
			RandomAccessFile raf = new RandomAccessFile(pdfFile, "r");
			FileChannel channel = raf.getChannel();
	        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
	        PDFFile pdf = new PDFFile(buf); 
	        raf.close();
	        return pdf;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Вычисление левых и правых возможных отступов на странице и поиск оптимальных
	 * @param pageAsImage страница 
	 * @param pageIndex порядковый номер страницы
	 */
	private static void findPageIndents(BufferedImage pageAsImage, int pageIndex){
		int pageWidth = pageAsImage.getWidth();
		int pageHeight = pageAsImage.getHeight();
		HashMap<Integer, Integer> sameLeftIndentsCount = new HashMap<>();
		HashMap<Integer, Integer> sameRightIndentsCount = new HashMap<>();
		for(int y = 0; y < pageHeight; y++){
			int leftIndent = 0;
			int rightIndent = 0;
			for(int x = 0; x < pageWidth; x++){
				int pixelValue = pageAsImage.getRGB(x,y) & 0xFF;
				if(pixelValue > 230) leftIndent++;
				else break; 
			}
			for(int x = pageWidth - 1; x >= 0; x--){
				int pixelValue = pageAsImage.getRGB(x,y) & 0xFF;
				if(pixelValue > 230) rightIndent++;
				else break; 
			}
			if(leftIndent < pageWidth - 10){
				if(!sameLeftIndentsCount.containsKey(leftIndent)) sameLeftIndentsCount.put(leftIndent, 1);
				else sameLeftIndentsCount.put(leftIndent, sameLeftIndentsCount.get(leftIndent) + 1);
			}
			if(rightIndent < pageWidth - 10){
				if(!sameRightIndentsCount.containsKey(rightIndent)) sameRightIndentsCount.put(rightIndent, 1);
				else sameRightIndentsCount.put(rightIndent, sameRightIndentsCount.get(rightIndent) + 1);
			}
		}
		
		HashMap<Integer, Integer> copy = new HashMap<>(sameLeftIndentsCount);
		for(Entry<Integer, Integer> entry1 : sameLeftIndentsCount.entrySet()){
			for(Entry<Integer, Integer> entry2 : copy.entrySet()){
				if(entry1.getKey() - entry2.getKey() != 0 && Math.abs(entry1.getKey() - entry2.getKey()) < 1){
					sameLeftIndentsCount.put(entry1.getKey(), sameLeftIndentsCount.get(entry1.getKey()) + copy.get(entry2.getKey()));
				}
			}
		}
		
		copy = new HashMap<>(sameRightIndentsCount);
		for(Entry<Integer, Integer> entry1 : sameRightIndentsCount.entrySet()){
			for(Entry<Integer, Integer> entry2 : copy.entrySet()){
				if(entry1.getKey() - entry2.getKey() != 0 && Math.abs(entry1.getKey() - entry2.getKey()) < 2){
					sameRightIndentsCount.put(entry1.getKey(), sameRightIndentsCount.get(entry1.getKey()) + copy.get(entry2.getKey()));
				}
			}
		}
		
		int popularLeftIndent = getPopularIndent(sameLeftIndentsCount);
		int popularRightIndent = getPopularIndent(sameRightIndentsCount);
		
		if(popularLeftIndent < popularRightIndent) leftJson.put(pageIndex + 1);
		
		else rightJson.put(pageIndex + 1);
		if(savingFolder != null) drawIndents(pageAsImage, popularLeftIndent, popularRightIndent, pageWidth, pageHeight);
	}
	
	/**
	 * Поиск оптимального отступа
	 * @param indents возможные отступы 
	 * @return оптимальный отступ
	 */
	private static int getPopularIndent(HashMap<Integer, Integer> indents){
		int popularIndent = 0;
		int count = 0;
		for(Entry<Integer, Integer> entry : indents.entrySet()){
			if(entry.getValue() > count){
				count = entry.getValue();
				popularIndent = entry.getKey();
			}
		}
		return popularIndent;
	}
	
	/**
	 * Конвертация pdf-страницы в изображение
	 * @param page pdf-страница 
	 * @return изображение
	 */
	private static BufferedImage getPageAsImage(PDFPage page){
		Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
        BufferedImage bufferedImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_BYTE_GRAY);
        Image image = page.getImage(rect.width, rect.height, rect, null, true, true);
        Graphics2D bufImageGraphics = bufferedImage.createGraphics();
        bufImageGraphics.drawImage(image, 0, 0, null);
		return bufferedImage;
	}
	
	/**
	 * Сохранение pdf-страницы как изображения
	 * @param pageAsImage изображение pdf-страницы
	 * @param pageIndex порядковый номер страницы 
	 */
	private static void savePageAsImage(BufferedImage pageAsImage, int pageIndex){
		try {
			File savingFolderFile = new File(savingFolder);
			if(!savingFolderFile.isDirectory()) savingFolderFile.mkdir(); 
			ImageIO.write(pageAsImage, "JPG", new File(savingFolderFile, pageIndex + ".jpg"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Отрисовка отступов на изображении
	 * @param pageAsImage изображение pdf-страницы
	 * @param leftIndent левый отступ
	 * @param rightIndent правый отступ
	 * @param pageWidth ширина страницы
	 * @param pageHeight высота страницы 
	 */
	private static void drawIndents(BufferedImage pageAsImage, int leftIndent, int rightIndent, int pageWidth, int pageHeight){
		Graphics2D bufImageGraphics = pageAsImage.createGraphics();
		bufImageGraphics.setColor(new Color(0, 0, 0));
		bufImageGraphics.drawLine(leftIndent, 0, leftIndent, pageHeight - 1);
		bufImageGraphics.drawLine(pageWidth - rightIndent, 0, pageWidth - rightIndent, pageHeight - 1);
	}
	
	/**
	 * Сохранение файла с результатами сортировки страниц
	 */
	private static void savePageSorting(){
		System.out.println("Сохранение результатов сортировки...");
		JSONObject sortingJson = new JSONObject();
		try {
			sortingJson.put("left", leftJson);
			sortingJson.put("right", rightJson);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		BufferedWriter output = null;
        try {
            File file = new File(pdfFile.getParent(), "sorting.json");
            output = new BufferedWriter(new FileWriter(file));
            try {
				output.write(sortingJson.toString(4));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } catch ( IOException e ) {
            e.printStackTrace();
        } finally {
            if ( output != null )
				try {
					output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        }
	}
}
