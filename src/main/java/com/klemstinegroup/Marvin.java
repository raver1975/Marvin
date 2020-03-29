package com.klemstinegroup;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import com.jhlabs.image.AbstractBufferedImageOp;
import com.jhlabs.image.GrayscaleFilter;
import com.jhlabs.image.LaplaceFilter;
import com.jhlabs.image.PosterizeFilter;
import com.jhlabs.image.SwimFilter;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class Marvin {
	int EDGES_THRESHOLD = 70;
	int LAPLACIAN_FILTER_SIZE = 5;
	int MEDIAN_BLUR_FILTER_SIZE = 7;
	int repetitions = 7; // Repetitions for strong cartoon effect.
	int ksize = 1; // Filter size. Has a large effect on speed.
	double sigmaColor = 9; // Filter color strength.
	double sigmaSpace = 7; // Spatial strength. Affects speed.
	int NUM_COLORS = 16;
	int gg = (256 / NUM_COLORS);

	SwimFilter sf = new SwimFilter();
	SwimFilter sf1 = new SwimFilter();
	LaplaceFilter lf = new LaplaceFilter();
	GrayscaleFilter gf = new GrayscaleFilter();
	PosterizeFilter glf = new PosterizeFilter();

	OpenCVFrameConverter converter=new OpenCVFrameConverter.ToIplImage();
	Java2DFrameConverter converterjava2d=new Java2DFrameConverter();

	CanvasFrame cf = new CanvasFrame("MyFaceIsMelting", 1);
	private float t1;
	private float t2;
	int frames = 1000;
	private int recthighgap=100;
	private int rectwidthgap=100;

	public Marvin() throws Exception {
		start();
	}

	public void start() throws Exception {
		sf.setAmount(20f);
		sf.setTurbulence(1f);
		sf.setEdgeAction(sf.CLAMP);
		sf1.setEdgeAction(sf1.CLAMP);
		sf1.setAmount(30f);
		sf1.setTurbulence(1f);
		sf1.setScale(300);
		sf1.setStretch(50);
		glf.setNumLevels(100);
		FrameGrabber grabber = new OpenCVFrameGrabber(0);
		grabber.start();
		cf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//Mat grabbedImage = converter.convert(grabber.grab());
		IplImage image = converter.convertToIplImage(grabber.grab());
		IplImage gray = IplImage.create(image.cvSize(), IPL_DEPTH_8U, 1);
		cvCvtColor(image, gray, CV_BGR2GRAY);
		IplImage edges = IplImage.create(gray.cvSize(), gray.depth(), gray.nChannels());
		IplImage temp = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
		while(true) {
			image = render(render(converter.convertToIplImage(grabber.grab()), sf), sf1);
//			image=grabber.grab();
			cvCvtColor(image, gray, CV_BGR2GRAY);
			cvSmooth(gray, gray, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
			cvLaplace(gray, edges, LAPLACIAN_FILTER_SIZE);
			cvThreshold(edges, edges, 80, 255, CV_THRESH_BINARY_INV);
//			 cvErode(edges, edges, null,2);
//			 cvDilate(edges, edges, null,1);
			// create contours around white regions
//			CvSeq contour = new CvSeq();
//			CvMemStorage storage = CvMemStorage.create();
//			cvSmooth(edges, edges, CV_MEDIAN, MEDIAN_BLUR_FILTER_SIZE, 0, 0, 0);
//			cvNot(edges,edges);
			
//			cvFindContours(edges, storage, contour,
//					Loader.sizeof(CvContour.class), CV_RETR_TREE,
//					com.googlecode.javacv.cpp.opencv_imgproc.CV_CHAIN_APPROX_SIMPLE);// CV_CHAIN_APPROX_SIMPLE);
//			// loop through all detected contours
//			cvZero(edges);
//			for (; contour != null && !contour.isNull(); contour = contour.h_next()) {
////				CvSeq approx = cvApproxPoly(contour,
////						Loader.sizeof(CvContour.class), storage, CV_POLY_APPROX_DP,
////						cvContourPerimeter(contour) * 0.001, 0);
////				CvRect rec = cvBoundingRect(contour, 0);
////					if (rec.height() > recthighgap && rec.width() > rectwidthgap) {
//				int area= Math.abs((int) cvContourArea(contour, CV_WHOLE_SEQ, -1));
//				int perimeter=(int) cvArcLength(contour, CV_WHOLE_SEQ, -1)+1;
//				
//				if (perimeter>100&&area>100&& area/perimeter==0){
////					System.out.println(area+"\t"+perimeter+"\t"+(area/perimeter));
////						CvMemStorage storage1 = CvMemStorage.create();
////						CvSeq convexContour = cvConvexHull2(contour, storage1,
////								CV_CLOCKWISE, 1);
//						cvDrawContours(edges, contour, CvScalar.WHITE,
//								CvScalar.WHITE, 127,1, 8);
//					}
//			}
//			cvNot(edges,edges);
			
			for (int i = 0; i < repetitions; i++) {
				cvSmooth(image, temp, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
				cvSmooth(temp, image, CV_BILATERAL, ksize, 0, sigmaColor, sigmaSpace);
			}
			temp = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
			cvZero(temp);
			
			
			
			cvCopy(image, temp, edges);
			sf.setTime(t1 += .02f);
			sf1.setTime(t2 += .02f);

			cf.showImage(converter.convert(render(temp, glf)));
		}
	}

	public static void main(String[] args) throws Exception {
		new Marvin();
	}

	public IplImage render(IplImage image, AbstractBufferedImageOp rf) {
		BufferedImage bi = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage bi2 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_INT_ARGB);
		bi.getGraphics().drawImage(converterjava2d.getBufferedImage(converter.convert(image)), 0, 0, null);
		rf.filter(bi, bi2);
		BufferedImage bi1 = new BufferedImage(image.width(), image.height(), BufferedImage.TYPE_3BYTE_BGR);
		bi1.getGraphics().drawImage(bi2, 0, 0, null);
		image =converter.convertToIplImage(converterjava2d.convert(bi1));
		return image;
	}

	public static FFmpegFrameRecorder getRecorder(String name, int width, int height, double frameRate, int quality) {
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(name, width, height);
		// recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
		// recorder.setFormat("mp4");
		// recorder.setFrameRate(frameRate);
		// recorder.setVideoBitrate(114 * 1024 * 1024);
		// recorder.setSampleRate(sampleRate);
		// recorder.setSampleFormat(sampleFormat);

		recorder.setVideoCodec(1);
		// recorder.setFormat("mp4");
		recorder.setFrameRate(frameRate);
		recorder.setVideoBitrate(quality * 1024 * 1024);
		try {
			recorder.start();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return recorder;
	}

	public static IplImage copy(IplImage image) {
		// if (img==null||img.isNull())return null;
		IplImage copy = null;
		if (image.roi() != null)
			copy = IplImage.create(image.roi().width(), image.roi().height(), image.depth(), image.nChannels());
		else
			copy = IplImage.create(image.cvSize(), image.depth(), image.nChannels());
		cvCopy(image, copy);
		return copy;
	}

}
