import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;


/**
 * 要处理的txt文件增大到286M，出现堆栈溢出的错误。
 * 解决方法：LinkedList设一个限值，当它的size大于限值的时候，就维持在这个限值上，不再增大，
 * 把index==0处的元素删去。后来发现一个问题，相邻(也可能是隔行的)的元素除时间不同外，内容都是相同的，这部分
 * 内容不保存，只保存一个。在恰当位置处理一下。
 * @author songchunyu
 */

public class TransTxtToHtml {
	public static final int ARRAY_LENGTH=1000;
	public static final int BACK_STEP=5;
	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws Exception {
		if(args.length!=2){//打印usage
			System.out.println(args.length + " usage:java TransTxtToHtml txtFilename DevicesID");
			return;
		}
		boolean PrintOrNot = true;
		boolean LastOne = true;
		String txtFilename=args[0];//要处理的文件名
		String DevicesName=args[1];
		String patternInfo="E/AndroidRuntime(";//异常标记
		String patternCrashPre="Process com.baidu.wearable (pid ";//崩溃标记前部分
		String patternCrashPost=") has died";//崩溃标记后部分
		String exceptionTagPattern=".*((java|android)\\..*Exception.*)";//匹配*java.*Exception*==异常标记
		Pattern exceptionTag=Pattern.compile(exceptionTagPattern);
		String errorTagPattern=".*((java|android)\\..*Error.*)";//匹配*java.*Error*==错误标记
		Pattern errorTag=Pattern.compile(errorTagPattern);
		Matcher matcher1,matcher2;
		LinkedList<String> tmpList=new LinkedList<String>();//暂存txt的内容
		LinkedList<String> CrashList = new LinkedList<String>();//存储第一句com.baidu
		CrashList.add("Hello World");
		String baidu = null;
		HashMap<String,Integer> result=new HashMap<String,Integer>();//存储各类崩溃异常标记和数目
		int count=0;
		try {
			Long begintime=new Date().getTime();
			BufferedReader br=new BufferedReader(new FileReader(txtFilename));
			PrintWriter pw=new PrintWriter(new OutputStreamWriter(
					new FileOutputStream("index_norepeat.html")), false);
			pw.println("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=gbk\" />" +
					"<title>monkey test result</title></head><body>");
			String line=br.readLine();
			int timeEndIndex="07-04 01:37:27.714".length()+1;
			int faceLength="g[face:3nj".length()+1;
			whileLoop1:
			while(line!=null){
				line=line.trim();
				//System.out.println("scy:"+line);
//				if(tmpList.size()>0&&!line.isEmpty()){
//					System.out.println("equals:"+tmpList.getLast().substring(timeEndIndex));
//					System.out.println("equals:"+line.substring(timeEndIndex));
//					System.out.println("equals:"+line.substring(timeEndIndex).equals(tmpList.getLast().substring(timeEndIndex)));
//				}
				//只有在line不为空，并且line除时间部分之外与上一个及上上个元素不相同时才保存到List中。
				if(line.isEmpty()){
					line=br.readLine();
					continue whileLoop1;
				}
				for(int i=tmpList.size()-1;i>=0&&i<BACK_STEP;i--){
					if(line.substring(timeEndIndex,line.length()-faceLength).
							equals(tmpList.get(i).substring(timeEndIndex,tmpList.get(i).length()-faceLength))){
						System.out.println("equal:"+tmpList.get(i).substring(timeEndIndex));
						line=br.readLine();
						continue whileLoop1;
					}						
				}
				tmpList.add(line);
				if(tmpList.size()>ARRAY_LENGTH)
					tmpList.remove(0);
				if(line.contains(patternCrashPre)&&line.contains(patternCrashPost)){//找到一个崩溃点
					count++;//计数器加1
					int size=tmpList.size();
					System.out.println(""+size);				
					while(size>0&&!tmpList.get(--size).contains(patternInfo)){;
					}//向前找到第一个包含错误信息的行
					while(size>0&&(tmpList.get(size).contains(patternInfo)||tmpList.get(size).isEmpty())){//继续向前找到不包含错误信息的行
						size--;
					}
					for(int i=size<0?0:size;i<tmpList.size();i++){//分类统计错误信息					
						String tmp=tmpList.get(i);
						if(!tmp.isEmpty()){
							matcher1=exceptionTag.matcher(tmp);
							matcher2=errorTag.matcher(tmp);
							if(matcher1.matches()){
								String value=matcher1.group(1);
								if(result.containsKey(value)){
									result.put(value, result.get(value)+1);
								}else{
									result.put(value, 1);
								}
								break;
							}else if(matcher2.matches()){
								String value=matcher2.group(1);
								if(result.containsKey(value)){
									result.put(value, result.get(value)+1);
								}else{
									result.put(value, 1);
								}
								break;
							}
						}
					}					
					tmpList.clear();//清除arrayList
				}
				line=br.readLine();
			}
			//上传数据到云图		
			
			///结果信息
			pw.print("<div style=\"color:#782217;font-size:20px;line-height:1.5em;background:#d3c99f; border-width:1px; border-style:solid; border-color:#000000;padding:10px;margin:10px\">");
			pw.print("<p>");
			pw.print(DevicesName + "\r\n This Monkey test crash count is :  "+count);
			pw.println("</p>");
			pw.print("</div>");
			///表格统计
			pw.print("<div style=\"color:#782217;font-size:20px;line-height:1.5em;background:#d3c99f; border-width:1px; border-style:solid; border-color:#000000;padding:10px;margin:10px\">");
			pw.print("<table width=\"100%\" align=\"center\" border=\"1\">");
			pw.print("<thead>");
			pw.print("<td width=\"20%\">index</td><td width=\"60%\">type</td><td width=\"20%\">count</td>");
			pw.print("</thead>");
			Iterator<String> it=result.keySet().iterator();
			for(int i=0;i<result.size();i++){
				String key=it.next();
				pw.print("<tr>");
				pw.print("<td width=\"20%\">"+(i+1)+"</td><td width=\"60%\">"+
						key+"</td><td width=\"20%\">"+result.get(key)+"</td>");
				pw.print("</tr>");
			}
			pw.print("</table>");
			pw.print("</div>");
			br.close();
			//打印详细错误信息
			count=0;
			tmpList.clear();
			br=new BufferedReader(new FileReader(txtFilename));
			line=br.readLine();
			whileLoop2:
			while(line!=null){
				line=line.trim();
				//只有在line不为空，并且line除时间部分之外与上一个元素不相同时才保存到List中。
				if(line.isEmpty()){
					line=br.readLine();
					continue whileLoop2;
				}
				for(int i=tmpList.size()-1;i>=0&&i<BACK_STEP;i--){
					if(line.substring(timeEndIndex,line.length()-faceLength).
							equals(tmpList.get(i).substring(timeEndIndex,tmpList.get(i).length()-faceLength))){
						line=br.readLine();
						continue whileLoop2;
					}						
				}
				tmpList.add(line);
				//给LinkedList一个限值
				if(tmpList.size()>ARRAY_LENGTH)
					tmpList.remove(0);
				if(line.contains(patternCrashPre)&&line.contains(patternCrashPost)){//找到一个崩溃点
					count++;//计数器加1
					int size=tmpList.size();				
					while(size>0&&!tmpList.get(--size).contains(patternInfo));//向前找到第一个包含错误信息的行
						//System.out.println(tmpList.get(size));
					while(size>0&&(tmpList.get(size).contains(patternInfo)||tmpList.get(size).isEmpty())){//继续向前找到不包含错误信息的行
						//System.out.println(tmpList.get(size));
							if((tmpList.get(size).contains("at com.baidu") || tmpList.get(size).contains("at com.nostra13")) && LastOne){
								LastOne = false;
								int CrashListSize = CrashList.size();
								for(int j=0;j<CrashListSize;j++){
									if(CrashList.get(j).contains(tmpList.get(size).substring(timeEndIndex,tmpList.get(size).length()-faceLength))){
										PrintOrNot = false;
									}
								}
								if(PrintOrNot){
									CrashList.add(tmpList.get(size).substring(timeEndIndex,tmpList.get(size).length()-faceLength));
								}
							}
						size--;
					}
					if(PrintOrNot){
						System.out.println("\\\\\\\\" + PrintOrNot);
						pw.print("<div style=\"font-size:14px;line-height:1.5em;background:#fffced; border-width:1px; border-style:solid; border-color:#000000;padding:10px;margin:10px\">");
						pw.print("<div style=\"line-height:1.5em;color:#782217;font-size:20px;padding:0px;margin:0px\">");
						pw.print("Crash number: "+count);
						pw.print("</div>");
						pw.print("<p>");
						for(int i=size<0?0:size;i<tmpList.size();i++){//将错误信息写入html文件						
						String tmp=tmpList.get(i);
							if(!tmp.isEmpty()){
								//System.out.println(tmp);
								pw.print(tmp);	
								pw.print("</br>");
							}
						}					
						pw.println("</p>");
						pw.print("</div>");
						}
						PrintOrNot=true;
						tmpList.clear();//清除arrayList
					
				}
				LastOne = true;
				line=br.readLine();
			}
			for(int k=0;k<CrashList.size();k++){
				System.out.println(CrashList.get(k));
				
			}
			System.out.println(CrashList.size());
			pw.println("</body></html>");
			pw.close();
			System.out.println("Html file has been created.");
			Long endtime=new Date().getTime();
			System.out.println("Processing time is "+(endtime-begintime));
		} catch (FileNotFoundException e) {
			System.out.println("file not found exception.");
			e.printStackTrace();
		}
		catch (IOException e) {
			System.out.println("io exception.");
			e.printStackTrace();
		}
	}
}
