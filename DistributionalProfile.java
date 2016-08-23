import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;


public class DistributionalProfile 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception 
	{
		// TODO Auto-generated method stub
		//*****************************************************************************
		//********************Creating Pesudo-Docs ************************************
		//*****************************************************************************
		FileReader in=null;
		FileWriter out2= new FileWriter("PDs.txt");
		FileWriter out3= new FileWriter("ppPDs.txt");
		HashMap<String, ArrayList<Integer>> Map2=new HashMap<String, ArrayList<Integer>>();
        int ind=0;

	      try{
	    	  	//********** Reading Corpus ****************
		         FileReader in3=new FileReader("cleanParallel.fa");
		         String [] corpus=new String[99923];
		         BufferedReader br=new BufferedReader(in3);
		         for (int l=0;l<99923;l++)
		        	 corpus[l]=br.readLine();
		         br.close();
		         in3.close();
		         //***************************************************************
				in=new FileReader("extract.sorted");
				br=new BufferedReader(in);
				String sentence;
		         String line;
		         String linecp;
		         String prev_line=null;
		         int arin=0;
		         int sen_id;
		         String PP;
		         System.out.println(ind);
		         ArrayList<Integer> line_nums=new ArrayList<Integer>(1);
		         while ((line = br.readLine())!=null) 
		         {
		        	 ind++;
		               	 linecp=line.substring(line.indexOf("|||")+4);
		        	 PP=line.substring(0,line.indexOf("|||"))+"||| "+linecp.substring(0,linecp.indexOf("|||"));
		        	 sen_id=Integer.parseInt(line.substring(line.lastIndexOf("|||")+4));
		        	 if(PP.equals(prev_line))
		        	 {
		        		 arin++;
		        	 }
		        	 else
		        	 {
			        		arin=0;
			        		if(prev_line!=null)
			        		{
			       				//************* Writing to file***
			        		  	out3.write(prev_line+"|||");
			        			for (int sen=0;sen<line_nums.size();sen++)
						    	  {
						    		  sentence=corpus[line_nums.get(sen)-1]+" ";
						    		  out2.write(sentence);
						    		  out3.write(sentence);

						    	  }						    	  
						    	  out2.write("\n");
						    	  out3.write("\n");
			        			//********************************
			        			line_nums=new ArrayList<Integer>(1);
			        		}
		        	 }
		        	 line_nums.add(sen_id);
		        	 prev_line=PP;
		         }
		         out3.write(prev_line+"|||");
     			for (int sen=0;sen<line_nums.size();sen++)
			    	  {
			    		  sentence=corpus[line_nums.get(sen)-1]+" ";
			    		  out2.write(sentence);
			    		  out3.write(sentence);

			    	  }						    	  
		         System.out.println(ind);
		         br.close();
		         in.close();
		         
			      out2.close();
			      out3.close();
		         
			}
			catch (Exception e)
		      {
				e.printStackTrace();
		         if (in != null) 
		         {
		            in.close();
		         }
		      }
	      
	      
	      
		
		
		
		//*****************************************************************************
		//********************Training Topic Models************************************
		//*****************************************************************************
				// Begin by importing documents from text to feature sequences
				ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

				// Pipes: lowercase, tokenize, remove stopwords, map to features
				pipeList.add( new CharSequenceLowercase() );
				pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
				pipeList.add( new TokenSequenceRemoveStopwords(new File("stoplists/fa_3.txt"), "UTF-8", false, false, false) );
				pipeList.add( new TokenSequence2FeatureSequence() );

				InstanceList instances = new InstanceList (new SerialPipes(pipeList));

				Reader fileReader = new InputStreamReader(new FileInputStream(new File("PDs.txt")), "UTF-8");
				instances.addThruPipe(new CsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
													   3, 2, 1)); // data, label, name fields
System.out.println("Done Adding Instances...");
				// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
				//  Note that the first parameter is passed as the sum over topics, while
				//  the second is 
				int numTopics = 50;
				ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

				model.addInstances(instances);

				// Use two parallel samplers, which each look at one half the corpus and combine
				//  statistics after every iteration.
				model.setNumThreads(2);

				// Run the model for 50 iterations and stop (this is for testing only, 
				//  for real applications, use 1000 to 2000 iterations)
				model.setNumIterations(1000);
				model.estimate();
System.out.println("Done Runnung the Model");
				// Show the words and topics in the first instance

				// The data alphabet maps word IDs to strings
				Alphabet dataAlphabet = instances.getDataAlphabet();
				
				FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
				LabelSequence topics = model.getData().get(0).topicSequence;
				
				Formatter out = new Formatter(new StringBuilder(), Locale.US);
				for (int position = 0; position < tokens.getLength(); position++) {
					out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
				}
				System.out.println(out);
				
				// Estimate the topic distribution of the first instance, 
				//  given the current Gibbs state.
				double[] topicDistribution = model.getTopicProbabilities(0);

				// Get an array of sorted sets of word ID/count pairs
				ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
				// Show top 5 words in topics with proportions for the first document
				for (int topic = 0; topic < numTopics; topic++) {
					Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
					
					out = new Formatter(new StringBuilder(), Locale.US);
					out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
					int rank = 0;
					while (iterator.hasNext() && rank < 5) {
						IDSorter idCountPair = iterator.next();
						out.format("%s (%.2f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
						rank++;
					}
					System.out.println(out);
				}
			
				// Create a new instance with high probability of topic 0
				StringBuilder topicZeroText = new StringBuilder();
				Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

				int rank = 0;
				while (iterator.hasNext() && rank < 5) {
					IDSorter idCountPair = iterator.next();
					topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
					rank++;
				}
				TopicInferencer inferencer = model.getInferencer();

	}

}
