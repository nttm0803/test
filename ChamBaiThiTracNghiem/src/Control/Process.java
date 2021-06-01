package Control;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.*;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.*;
import static org.opencv.imgproc.Imgproc.drawContours;

public class Process {
	private Rect roi, roi_1, roi_2, mssv, code, roi_1_1, roi_1_2, roi_mssv;
	private Mat source,gray, thresh, blur, canny, adaptiveThresh, hierarchy, drop;
	private List<Rect> list10rect, list40Ans, list160Options, list10RectNumberStudent;
	private List<MatOfPoint> contours, bubbles;
	private boolean[][] answer40, ansMssv, ansCode;
	private int colsMSSV = 6, colsCode = 3, numberChoose;
	
	public Process() {
		source = null;
		hierarchy = new Mat();
		contours = new ArrayList<>();
		bubbles = new ArrayList<>();
		list10rect = new ArrayList<Rect>();
		list40Ans = new ArrayList<Rect>();
		list160Options = new ArrayList<Rect>();
//		đáp án
		answer40 = new boolean[40][4];
		// mssv
		ansMssv = new boolean[10][6];
		// code
		ansCode = new boolean[10][3];
	}

	// Scan anh + chuyen doi anh, numberchoose là số câu muốn chấm bắt đầu từ 1
	public void scan(Mat source, int numberChoose) throws Exception {
		this.source = source;
		double percent = 20;
		this.numberChoose = numberChoose;
		gray = new Mat(source.size(), CV_8UC1);
		cvtColor(source, gray, COLOR_BGR2GRAY);

		blur = new Mat(gray.size(), CV_8UC1);
		GaussianBlur(gray, blur, new Size(5, 5), 0);

		canny = new Mat(blur.size(), CV_8UC1);
		Canny(blur, canny, 160, 20);

		adaptiveThresh = new Mat(canny.rows(), gray.cols(), gray.type());
		adaptiveThreshold(canny, adaptiveThresh, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);
		
		findReactangle();
		//		tìm mã số sinh viên
		findChooseCodeOrStudentNumber(percent, mssv, colsMSSV, this.ansMssv);
		//		tìm mã đề
		findChooseCodeOrStudentNumber(percent, code, colsCode, this.ansCode);
		find8RectIn2Rect();
		findLineIn8Col();
		splitNumberQuestion();
		//		tìm đáp án
		dropAns(percent);
	}

	// tim ra khung mssv, dap an, ma de
	private void findReactangle() throws Exception {
		findContours(adaptiveThresh.clone(), contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);
		int w_curr = 0, h_curr = 0;
		ArrayList<Integer> ans_blocks = new ArrayList<Integer>();
		ArrayList<Rect> listMat = new ArrayList<Rect>();

		for (int i = 0; i < contours.size(); i++) {
			w_curr = boundingRect(contours.get(i)).width;
			h_curr = boundingRect(contours.get(i)).height;
			if ((w_curr * h_curr) > 10000) {
				ans_blocks.add(i);
			}
		}

		List<Integer> check = new ArrayList<Integer>();
		for (int i = 0; i < ans_blocks.size(); i++) {
			Rect temp = boundingRect(contours.get(ans_blocks.get(i)));
			int dt = temp.width * temp.height;
			if (!check.contains(dt)) {
				listMat.add(temp);
				check.add(dt);
			}

		}

		Collections.sort(listMat, new Comparator<Rect>() {
			@Override
			public int compare(Rect o1, Rect o2) {
				return (o2.width * o2.height) - (o1.width * o1.height);
			}
		});

		int widthRect = 0, heightRect = 0;
		for(Rect rec : listMat) {
			widthRect = rec.width;
			heightRect = rec.height;
			if ((heightRect > 322 && heightRect < 328) && (widthRect < 71 && widthRect > 65)) 
				code = rec;
			if ((heightRect > 322 && heightRect < 328) && (widthRect < 135 && widthRect > 129)) 
				mssv = rec;
			if ((heightRect > 703 && heightRect < 708) && (widthRect < 293 && widthRect > 288)) 
				roi_1 = rec;
			if ((heightRect > 709 && heightRect < 714) && (widthRect < 299 && widthRect > 293)) 
				roi_2 = rec;
		}
		

	}
	// Xac dinh tung khung trong 2 khung lon
	public void find8RectIn2Rect() {
		int height = 0;
		int step = roi_1.height / 4;
		int y = roi_1.y;
		for (int i = 0; i < 4; i++) {
			roi_1_1 = new Rect(roi_1.x, y, roi_1.width, height = roi_1.height / 4 + 4);
			list10rect.add(roi_1_1);
			y += height - 3;
			height += step;
		}
		height = 0;
		step = roi_2.height / 5;
		y = roi_2.y;
		for (int i = 0; i < 4; i++) {
			roi_1_2 = new Rect(roi_2.x, y, roi_2.width, height = roi_2.height / 4 + 4);
			list10rect.add(roi_1_2);
			y += height - 4;
			height += step;
		}
	}
	// Moi khung tim ra  5 dong dap an
	public void findLineIn8Col() {
		for (int i = 0; i < list10rect.size() / 2; i++) {
			int y_Temp = list10rect.get(i).y + 6;
			for (int j = 0; j < 5; j++) {
				Rect rec = new Rect(list10rect.get(i).x, y_Temp, list10rect.get(i).width, 25);
				list40Ans.add(rec);
				y_Temp += 25 + 8;
			}
		}
		for (int i = list10rect.size() / 2; i < list10rect.size(); i++) {
			int y_Temp = list10rect.get(i).y + 7;
			for (int j = 0; j < 5; j++) {
				Rect rec = new Rect(list10rect.get(i).x, y_Temp, list10rect.get(i).width, 26);
				list40Ans.add(rec);
				y_Temp += 25 + 10;

			}
		}
	}
	// tien xy ly dau dong va cuoi dong
	public void splitNumberQuestion() {
		List<Rect> list40Temp = new ArrayList<Rect>();
		for (int i = 0; i < list40Ans.size(); i++) {
			Rect rec = new Rect(list40Ans.get(i).x + 50, list40Ans.get(i).y, list40Ans.get(i).width - 53,
					list40Ans.get(i).height);
			list40Temp.add(rec);
		}
		list40Ans.clear();
		list40Ans.addAll(list40Temp);

	}
	// Cat 1 dong dap an ra thanh tung o dap an A, B, C, D va tim ra dap an duoc to, number choose là số câu muốn lấy ra
	public void dropAns(double percent) {
		int widthCount = 0;
		int heightCount = 0;
		int pixcelBlack = 0;
		int blackPercent = 0;
		thresh = new Mat(gray.rows(), gray.cols(), gray.type());
		threshold(gray, thresh, 150, 255, THRESH_BINARY);
		// duyệt qua 40 dòng
		for (int i = 0; i < this.numberChoose; i++) {
			int x = list40Ans.get(i).x + 10;
			// mỗi dòng duyệt qua 4 đáp án A,B,C,D
			
			for (int j = 0; j < 4; j++) {
				Rect rec = new Rect(x, list40Ans.get(i).y, list40Ans.get(i).width / 4 - 30, list40Ans.get(i).height);
				widthCount = rec.width;
				heightCount = rec.height;
				x += list40Ans.get(0).width / 4 + 5;
				// tìm pixcel đen
				pixcelBlack = (widthCount * heightCount) - Core.countNonZero(thresh.submat(rec));
				// phần trăm pixcel đen trên tổng số pixcel
				blackPercent = (pixcelBlack * 100 / (widthCount * heightCount));
				list160Options.add(rec);
				// tô đậm hơn quy định thì tính câu đúng
				if (blackPercent > percent) {
					answer40[i][j] = true;
				}else
					answer40[i][j] = false;
			}
		}
		kiemTra();
		

	}
	
	// kiem tra to 2 o trong 1 dong dap an thi cau do sai
	public void kiemTra() {
		int count = 0;
		for (int i = 0; i < 40; i++) {
			for (int j = 0; j < 4; j++) {
				if (answer40[i][j] == true)
					count++;
				if (count > 1) {
					answer40[i][0] = false;
					answer40[i][1] = false;
					answer40[i][2] = false;
					answer40[i][3] = false;
					break;
				}
			}
			count = 0;
		}
	}
	// kiem tra to sai ma so sinh vien, to 2 trong cung 1 cot
	public boolean checkMSSV() {
		int count = 0;
		for (int i = 0; i < ansMssv[0].length; i++) {
			for (int j = 0; j < ansMssv.length; j++) {
				if (ansMssv[j][i] == true)
					count++;
				if (count > 1 || count == 0)
					return false;
			}
		}
		return true;
	}

	// kiem tra to 2 o trong 1 cot ma de
	public boolean checkCode() {
		int count = 0;
		for (int i = 0; i < ansCode[0].length; i++) {
			for (int j = 0; j < ansCode.length; j++) {
				if (ansCode[j][i] == true)
					count++;
				if (count > 1 || count == 0)
					return false;
			}
		}
		return true;
	}

	// tim ra ma de va mssv duoc to
	public void findChooseCodeOrStudentNumber(double percent, Rect input, int cols, boolean[][] temp) {
		int y = input.y, x = input.x, widthCell = 22, heightCell = 32, widthCount = 0, heightCount = 0, pixcelBlack = 0;
				
		double blackPercent = 0;
		thresh = new Mat(gray.rows(), gray.cols(), gray.type());
		threshold(gray, thresh, 150, 255, THRESH_BINARY);
		for (int i = 0; i < cols; i++) {
			for (int j = 0; j < 10; j++) {
				roi_mssv = new Rect(x, y, widthCell, heightCell);
				y += heightCell;
				widthCount = roi_mssv.width;
				heightCount = roi_mssv.height;
				pixcelBlack = (widthCount * heightCount) - Core.countNonZero(thresh.submat(roi_mssv));
				blackPercent = (pixcelBlack * 100 / (widthCount * heightCount)) ;
				
				if (blackPercent > percent)
					temp[j][i] = true;
				else
					temp[j][i] = false;
			}
		
			y = input.y;
			x += widthCell;
		}
		
	}

	// lay ra danh sach dap an 
	public Map<Integer, String> getAnswer() {
		Map<Integer, String> listAnswer = new HashMap<Integer, String>();
		for (int i = 0; i < this.numberChoose; i++) {
			for (int j = 0; j < 4; j++) {
				if (answer40[i][j] == true) {
					switch (j) {
						case 0: 
							listAnswer.put(i + 1, "A");
							break;
						case 1:
							listAnswer.put(i + 1, "B");
							break;
						case 2:
							listAnswer.put(i + 1, "C");
							break;
						case 3:
							listAnswer.put(i + 1, "D");
							break;
					}
					break;
				}
			}
		}
		return listAnswer;
	}
	// lay ra ma de
	public String getCode() {
		String codeStr = "";
		for (int i = 0; i < ansCode[0].length; i++) {
			for (int j = 0; j < ansCode.length; j++) {
				if (ansCode[j][i] == true) {
					codeStr += j + " ";
				}
			}
		}
		return codeStr;
	}

	// lay ra ma de
	public String getMssv() {
		String mssvStr = "";
		for (int i = 0; i < ansMssv[0].length; i++) {
			for (int j = 0; j < ansMssv.length; j++) {
				if (ansMssv[j][i] == true) {
					mssvStr += j + " ";
					break;
				}
			}
		}
		return mssvStr;
	}
	
}
