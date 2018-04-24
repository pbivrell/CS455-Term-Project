import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.commons.math3.util.Precision;
import java.util.*;


public class MapBuilder {
  public static void main(String[] args) throws Exception{
    SparkConf conf = new SparkConf().setAppName("Map Builder");
    System.out.println("Starting Up");

    ServerSocket s = new ServerSocket(47339);
    JavaSparkContext sc = new JavaSparkContext(conf);

    while(true){
    	Socket socket = s.accept();
    	ParseThread thread = new ParseThread(socket, sc);
    	Thread t = new Thread(thread);
    	t.start();
    }
    
  }

  public static class ParseThread implements Runnable{

  	private Socket socket;
  	private JavaSparkContext sc;

  	public ParseThread(Socket socket, JavaSparkContext sc){
  		this.sc = sc;
  		this.socket = socket;
  	}

  	@Override
  	public void run(){
    		System.out.println("Connection Accepted");
    		String currMessage = "";
    		List<String> body = new ArrayList<>();
    		BufferedReader br;
    		BufferedWriter bw;

    		try{
    			br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    			bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));


    			while((currMessage = br.readLine()) != null){
				System.out.println(currMessage);
				/*if(currMessage.contains(",")){
					body.add(currMessage);
				}
				if(currMessage.isEmpty() || currMessage == null){
					break;
				}*/
		
    				if(!currMessage.isEmpty() && Character.isDigit(currMessage.charAt(0)) ){
					System.out.println("Reading " + currMessage + " lines");
    					int lines = Integer.parseInt(currMessage);
    					for(int i = 0; i < lines - 1; i++){
    						if(currMessage != null && !currMessage.isEmpty()){
    							currMessage = br.readLine();
    							body.add(currMessage);
							System.out.println(currMessage);
    						}
    					}
    					break;
    				}

    			}

    			System.out.println("End of read");

    			JavaRDD<String> bodyRDD = sc.parallelize(body);

    			/*JavaRDD<Double> trimmed = bodyRDD.filter(line -> (line.contains("latitudeE7") || line.contains("longitudeE7")))
    			.map(chosen -> chosen.split(":")[1])
    			.map(coord -> coord.substring(1,coord.length()-1))
    			.map(val -> Double.parseDouble(val))
    			.map(div -> (div / 10000000));*/

			
 			JavaRDD<String> trimmed = bodyRDD.map(sig -> Double.toString(Precision.round(Double.parseDouble(sig.split(",")[0]), -3) ) + "," + Double.toString(Precision.round(Double.parseDouble(sig.split(",")[1]), -3) ))
            .distinct().map(pairs -> Double.toString(Double.parseDouble(pairs.split(",")[0]) / 10000000) + "," + Double.toString(Double.parseDouble(pairs.split(",")[1]) / 10000000) );
    	

    			List<String> output = trimmed.collect();
    			String result = "";

    			for(int i = 0; i < output.size(); i++){
    				result += output.get(i) + "\n";
    			}


			String lenHead = output.size() + "\n";
    			System.out.println(lenHead + result);
    			bw.write(lenHead + result);
                bw.newLine();
    			bw.flush();
    			
    			br.close();
    			bw.close();
    			socket.close();
    		}catch(Exception e){}
  	}
  }


}
