package org.knowrob.interfaces.PDF_factory;

import java.io.*;
import java.util.StringTokenizer;
import java.lang.Double;
import java.util.ArrayList;
import java.text.DecimalFormat;

import org.ros.message.Duration;
import org.ros.message.Time;
import org.knowrob.utils.ros.RosUtilities;
import org.knowrob.tfmemory.TFMemory;
import tfjava.StampedTransform;

/**
 * 2D latex-based plan summary from log files 
 *
 * @author asil@cs.uni-bremen.de
 *
 */
public class PDF_factory 
{
        String path_of_knowrob_plan_summary;
	String path_for_generated_latex;

	public PDF_factory()
	{
		path_of_knowrob_plan_summary = RosUtilities.rospackFind("knowrob_plan_summary");
	}

	public PDF_factory(String path_for_generated_latex)
	{
		this();
		this.setPathForGeneratedLatex(path_for_generated_latex);
	}

	public void setPathForGeneratedLatex(String path_for_generated_latex)
	{
		this.path_for_generated_latex = path_for_generated_latex;
	}

	public boolean generatePDF()
	{
		String path = null;
		try {       	
			String[] parsed_path = path_for_generated_latex.split("/");
			String path_for_generated_pdf = path_for_generated_latex.replaceAll(".tex", ".pdf");

			String path_without_pdf_name = "";
			for(int i = 0; i < parsed_path.length -1; i++)
				path_without_pdf_name += "/" + parsed_path[i];

			Process p = Runtime.getRuntime().exec("pdflatex " + path_for_generated_latex, null, new File(path_without_pdf_name));
			p.waitFor();
		}
		catch (Exception e)
		{
		       e.printStackTrace();
		       return false;
			
		}
		return true;
	}

        public boolean addPlanTrajectory(String tflink, String starttime, String endtime, double interval)
	{
		try
		{

			String identifier, formattedTime;
			String tflink_ = (tflink.startsWith("/") ? tflink : "/"+tflink);

			double t0 = parseTime_d(starttime);
			double t1 = parseTime_d(endtime);

			ArrayList<StampedTransform> sts = new ArrayList<StampedTransform>();
			for (double i = t0; i <= t1; i += interval) {
				formattedTime = new DecimalFormat("###.###").format(i);
				// read marker from Prolog
				StampedTransform m = null;
				try {
					m = readLinkMarkerFromProlog(tflink_, "'timepoint_" + formattedTime + "'", 0);
				}
				catch(Exception e) {
					System.out.println("Unable to read marker for time point '" + formattedTime + "'.");
				}
				if(m==null) continue;
				else sts.add(m);
			}

			if(sts.size() > 0)
			{
				FileReader fr = new FileReader(path_for_generated_latex);  
				BufferedReader br = new BufferedReader(fr);  

				FileWriter fstream = new FileWriter(path_of_knowrob_plan_summary + "/latex/temp_output.tex");
				BufferedWriter out = new BufferedWriter(fstream);

				String line = null;

				while((line=br.readLine()) != null) 
				{  
					out.write(line);
					out.newLine(); 


					if(line.length() > 8 && line.substring(0,8).equals("\\addwall")  || line.length() > 18 
					&& line.substring(0,18).equals("\\highlightposition"))
						break; 
		    		}

				for(int i = 0; i + 3 < sts.size(); i = i + 3)
				{
					StampedTransform m1 = sts.get(i);
					StampedTransform m2 = sts.get(i + 1);
					StampedTransform m3 = sts.get(i + 2);
					StampedTransform m4 = sts.get(i + 3);

					out.write("\\addtrajectory{" + m1.getTranslation().y + "}{" + m1.getTranslation().x 
						+ "}{" + m2.getTranslation().y + "}{" + m2.getTranslation().x 
						+ "}{" + m3.getTranslation().y + "}{" + m3.getTranslation().x
						+ "}{" + m4.getTranslation().y + "}{" + m4.getTranslation().x
						+ "}\n");
				}

				while((line=br.readLine()) != null) 
				{  
					out.write(line);
					out.newLine(); 
				}
				br.close();			
				out.close();

				// Once everything is complete, delete old file..
	      			File oldFile = new File(path_for_generated_latex);
	      			oldFile.delete();

	     			// And rename tmp file's name to old file name
	      			File newFile = new File(path_of_knowrob_plan_summary + "/latex/temp_output.tex");
	      			newFile.renameTo(oldFile);
				}

		}
		catch (Exception e)
		{
      			System.err.println("Error: " + e.getMessage());
			return false;
    		}
		return true;

	}

	public boolean locateRobotPositions(String[][] locations)
	{
		try
		{

			FileReader fr = new FileReader(path_for_generated_latex);  
        		BufferedReader br = new BufferedReader(fr);  

			FileWriter fstream = new FileWriter(path_of_knowrob_plan_summary + "/latex/temp_output.tex");
        		BufferedWriter out = new BufferedWriter(fstream);

			String line = null;

			while((line=br.readLine()) != null) 
			{  
        			out.write(line);
				out.newLine(); 


				if(line.length() > 8 && line.substring(0,8).equals("\\addwall"))
					break; 
            		}

			for(int x = 0; x < locations.length; x++)
			{
				String current_x = locations[x][0];
				String current_y = locations[x][1];

				out.write("\\highlightposition{"+ current_x + "}{" + current_y + "}{" + x + "}{red}\n");
			}

			while((line=br.readLine()) != null) 
			{  
        			out.write(line);
				out.newLine(); 
			}
			br.close();			
			out.close();

			// Once everything is complete, delete old file..
      			File oldFile = new File(path_for_generated_latex);
      			oldFile.delete();

     			// And rename tmp file's name to old file name
      			File newFile = new File(path_of_knowrob_plan_summary + "/latex/temp_output.tex");
      			newFile.renameTo(oldFile);

		}
		catch (Exception e)
		{
      			System.err.println("Error: " + e.getMessage());
			return false;
    		}
		return true;

	}

	public boolean createLatex(String[][] objects, String[][] walls)
	{
		try
		{
			String path_of_knowrob_plan_summary = RosUtilities.rospackFind("knowrob_plan_summary");

			FileReader fr = new FileReader(path_of_knowrob_plan_summary + "/latex/base.tex");  
        		BufferedReader br = new BufferedReader(fr);  

			FileWriter fstream = new FileWriter(path_for_generated_latex);
        		BufferedWriter out = new BufferedWriter(fstream);


			String line = null;
			while((line=br.readLine()) != null) 
			{  
        			out.write(line);
				out.newLine();  
            		}  

			for(int x = 0; x < walls.length; x++)
			{
				double current_x = getRealValue(walls[x][1]);
				double current_y = getRealValue(walls[x][2]);
				double current_width = getRealValue(walls[x][0]);
				int orientation = (int) getRealValue(walls[x][3]);				

				out.write("\\addwall{" + orientation + "}{" + current_x + "}{" + current_y + "}{" 
						+ current_width + "}\n");

				/*if(walls[x][3].equals("literal(type('http://www.w3.org/2001/XMLSchema#double','-1.0'))"))
				{
					out.write("\\addwall{" + current_y + "}{" + (current_x - current_width/2) + "}{" + current_y + "}{" 
						+ (current_x + current_width/2) + "}\n");
				}
				else
				{	
					out.write("\\addwall{" + (current_y - current_width/2)  + "}{" + current_x + "}{" + (current_y + current_width/2) + 
						"}{" + current_x + "}\n");
				}*/
			}

	
			for(int x = 0; x < objects.length; x++)
			{

				String furniture_name = getName(objects[x][0]);

				double current_x = getRealValue(objects[x][4]);
				double current_y = getRealValue(objects[x][5]);
				double current_width = getRealValue(objects[x][1]);
				double current_height = getRealValue(objects[x][2]);
				double current_depth = getRealValue(objects[x][3]);

				if(objects[x][6].equals("literal(type('http://www.w3.org/2001/XMLSchema#double','-1.0'))"))
					out.write("\\addfurniture{" + current_x + "}{" + current_y + "}{" + current_width + "}{" + current_height + "}{" + furniture_name + "}\n");
				else
					out.write("\\addfurniture{" + current_x + "}{" + current_y + "}{" + current_width + "}{" + current_depth + "}{" + furniture_name + "}\n");
			}

			out.write("\\end{tikzpicture}\n\\end{frame}\n\\end{document}");
			br.close();			
			out.close();
		}
		catch (Exception e)
		{
      			System.err.println("Error: " + e.getMessage());
			return false;
    		}
		return true;

	}


	public double getRealValue(String data)
	{
		StringTokenizer data_tokenizer = new StringTokenizer(data, "'");

		String data_tokenized = data_tokenizer.nextToken();
	        data_tokenized = data_tokenizer.nextToken();
		data_tokenized = data_tokenizer.nextToken();
		data_tokenized = data_tokenizer.nextToken();

		return Double.parseDouble(data_tokenized);

	}

	public String getName(String data)
	{
		data = data.replace('\'',' ');
		data = data.replace('_','-');

		StringTokenizer data_tokenizer = new StringTokenizer(data, "#");

		String data_tokenized = data_tokenizer.nextToken();
	        data_tokenized = data_tokenizer.nextToken();
		return data_tokenized;

	}

        /**
	 * Parses String with common time format 'timepoint_%d'
	 * and returns a double precision number that represents
	 * the time passed since 1970.
	 */
	double parseTime_d(String timepoint) {
		String x[] = timepoint.split("timepoint_");
		// Also allow input strings without 'timepoint_' prefix
		String ts = (x.length==1 ? x[0] : x[1]);
		return Double.valueOf(ts.replaceAll("[^0-9.]", ""));
	}

        /**
	 * Parses String with common time format 'timepoint_%d'
	 * and returns a Time object.
	 */
	Time parseTime(String timepoint) {
		double posix_ts = parseTime_d(timepoint);
		Time time = new Time();
		time.secs = (int)posix_ts;
		time.nsecs = (int) (1E9 * (posix_ts - ((int) posix_ts)));
		return time;
	}

	StampedTransform readLinkMarkerFromProlog(String link, String timepoint, int marker_type) {
		StampedTransform tr = null; 
		// Lookup TF transform that corresponds to specified link
		try {
			tr = TFMemory.getInstance().lookupTransform("/map", link, parseTime(timepoint));
			if(tr==null) {
				System.out.println("TF data missing for '" + link + "' " + timepoint + " missing in mongo.");
				return null;
			}
		}
		catch (Exception e) {
			System.out.println("Unable to lookup transform for '" + link + "'.");
		}

		return tr;
	}

	public static void main(String[] args)
	{
		/*String[] objects = new String[3];

		double[] x_coord = new double[3];
		double[] y_coord = new double[3];
		double[] width = new double[3];
		double[] height = new double[3];

		double[] x_wall = new double[6];
		double[] y_wall = new double[6];

		objects[0] = "table";
		objects[1] = "door";
		objects[2] = "chair";

		x_coord[0] = 1;
		x_coord[1] = 2;
		x_coord[2] = 3;

		y_coord[0] = 4;
		y_coord[1] = 5;
		y_coord[2] = 6;

	
		width[0] = 1;
		width[1] = 2;
		width[2] = 3;

		height[0] = 4;
		height[1] = 5;
		height[2] = 6;		
		
		x_wall[0] = 1;
		x_wall[1] = 2;
		x_wall[2] = 3;
		x_wall[3] = 4;
		x_wall[4] = 5;
		x_wall[5] = 6;

		y_wall[0] = 1;
		y_wall[1] = 2;
		y_wall[2] = 3;
		y_wall[3] = 4;
		y_wall[4] = 5;
		y_wall[5] = 6;*/

		String[][] objects = new String[3][6];
		objects[0][0] = "1#table";
		objects[1][0] = "1#door";
		objects[2][0] = "1#chair";
		
		objects[0][4] = "0'1'2'1";
		objects[1][4] = "0'1'2'2";
		objects[2][4] = "0'1'2'3";

		objects[0][5] = "0'1'2'4";
		objects[1][5] = "0'1'2'5";
		objects[2][5] = "0'1'2'6";

		objects[0][1] = "0'1'2'1";
		objects[1][1] = "0'1'2'2";
		objects[2][1] = "0'1'2'3";

		objects[0][2] = "0'1'2'4";
		objects[1][2] = "0'1'2'5";
		objects[2][2] = "0'1'2'6";

		objects[0][3] = "0'1'2'4";
		objects[1][3] = "0'1'2'5";
		objects[2][3] = "0'1'2'6";		
		

		String[][] walls = new String[6][4];
		walls[0][0] = "0'1'2'1";
		walls[1][0] = "0'1'2'2";
		walls[2][0] = "0'1'2'3";
		walls[3][0] = "0'1'2'4";
		walls[4][0] = "0'1'2'5";
		walls[5][0] = "0'1'2'6";

		walls[0][1] = "0'1'2'1";
		walls[1][1] = "0'1'2'2";
		walls[2][1] = "0'1'2'3";
		walls[3][1] = "0'1'2'4";
		walls[4][1] = "0'1'2'5";
		walls[5][1] = "0'1'2'6";

		walls[0][2] = "0'1'2'1";
		walls[1][2] = "0'1'2'2";
		walls[2][2] = "0'1'2'3";
		walls[3][2] = "0'1'2'4";
		walls[4][2] = "0'1'2'5";
		walls[5][2] = "0'1'2'6";

		walls[0][3] = "0'1'2'1";
		walls[1][3] = "0'1'2'2";
		walls[2][3] = "0'1'2'3";
		walls[3][3] = "0'1'2'4";
		walls[4][3] = "0'1'2'5";
		walls[5][3] = "0'1'2'6";
		
		PDF_factory pdf = new PDF_factory();

		pdf.createLatex(objects, walls);

	}

}
