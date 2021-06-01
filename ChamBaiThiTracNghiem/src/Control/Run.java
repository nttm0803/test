package Control;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.plaf.FileChooserUI;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

public class Run {

	public static void main(String[] args) throws Exception {
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Process pro = new Process();
		JFileChooser jcs = new JFileChooser();
		jcs.setMultiSelectionEnabled(true);
		int selected = jcs.showOpenDialog(null);
		if (selected == JFileChooser.APPROVE_OPTION) {
			File[] files = jcs.getSelectedFiles();
			for (File file : files) {
				Mat s = Imgcodecs.imread(file.getAbsolutePath());
				pro.scan(s, 10);
				System.out.println("MSSV " + pro.getMssv().trim());
				System.out.println("Mã đề " + pro.getCode().trim());
				System.out.println("Đáp án " + pro.getAnswer());
			}
		}
		
		
	}	

}
