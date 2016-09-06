import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;


public class DistributionalProfile 
{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception 
	{
		// TODO Auto-generated method stub
		//*****************************************************************************
		//********************Creating Pseudo-Docs ************************************
		//*****************************************************************************
		FileReader in=null;
		FileWriter out2= new FileWriter("PDs.txt");
		FileWriter out3= new FileWriter("ppPDs.txt");
        int ind=0;

	      try{
	    	  	//********** Reading Corpus ****************
	    	  	 int corpus_size=99923;
		         FileReader in3=new FileReader("cleanParallel.fa");
		         String [] corpus=new String[corpus_size];
		         BufferedReader br=new BufferedReader(in3);
		         for (int l=0;l<corpus_size;l++)
		        	 corpus[l]=br.readLine();
		         br.close();
		         in3.close();
		         //***************************************************************
				in=new FileReader("extract.sorted");
				br=new BufferedReader(in);
				String sentence;
		         String line,linecp,PP,prev_line=null;
		         int sen_id,arin=0;
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
						    	out2.write(0+"\tX\t");
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
		         out2.write(0+"\tX\t");
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
				System.out.println("Adding Instances...");
				Reader fileReader = new InputStreamReader(new FileInputStream(new File("PDs.txt")), "UTF-8");
				instances.addThruPipe(new CsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
													   3, 2, 1)); // data, label, name fields

				// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
				//  Note that the first parameter is passed as the sum over topics, while
				//  the second is 
				int numTopics = 50;
				ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

				model.addInstances(instances);

				// Use two parallel samplers, which each look at one half the corpus and combine
				//  statistics after every iteration.
				model.setNumThreads(2);
				System.out.println("Estimating...");

				// Run the model for 50 iterations and stop (this is for testing only, 
				//  for real applications, use 1000 to 2000 iterations)
				model.setNumIterations(1000);
				model.estimate();
				File f = new File("topicmodel");
				model.write(f);
				// ShowD the words and topics in the first instance

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
				//ArrayList<TreeSet<IDSorter>> topicSortedProbs = model.get
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
				
				//************************************************************************
				//************************************************************************
				in = null;
			    FileWriter out1 = null;	
			    //***********************************************************************
			    try {
			         in = new FileReader("phrase-table");
			         out1 = new FileWriter("phrase-table-topics");
			         
			         BufferedReader br=new BufferedReader(in);
			         String line;
			         int index=0;
			         while ((line = br.readLine())!=null) 
			         	{
			        	 	// Estimate the topic distribution of this instance, 
							//  given the current Gibbs state.
							topicDistribution = model.getTopicProbabilities(index);
							line=line+"{{Topics ";
							for (int i=0;i<numTopics;i++)
			        			 {
			        				 line=line+String.format("%.5f", topicDistribution[i])+" ";
			        			 }
							out1.write(line+"}}\n");
							index++;
			         	}
			      	 }
			      finally 
			      {
			         if (in != null) 
			         {
			            in.close();
			         }
			         if (out1 != null)
			         {
			            out1.close();
			         }
			      }	
			  //************************************************************************		   
			      try {
				         in = new FileReader("Dev.fa");
				         out1 = new FileWriter("Dev.fa.topics.ixml");
				         
				         BufferedReader br=new BufferedReader(in);
				         String line;
				         while ((line = br.readLine())!=null) 
				         	{
				        	 	InstanceList testing1 = new InstanceList(instances.getPipe());
					     		testing1.addThruPipe(new Instance(line, null, "test instance", null));

					        	 double[] testProbs = inferencer.getSampledDistribution(testing1.get(0), 10, 1, 5);
					        	 
					        	 int j;
					        	 String output="<seg topic=\"";
					 			 for (j=0;j<numTopics;j++) 
					 				 output=output+Integer.toString(j)+" "+String.format("%.5f", testProbs[j])+" ";
					 			 output=output+("\">"+line+"</seg>\n");
					 			 out1.write(output);
				         	}
				      	 }
				  finally 
				      {
				         if (in != null) 
				         {
				            in.close();
				         }
				         if (out1 != null)
				         {
				            out1.close();
				         }
				      }	
			     
			    //***********************************************************************
			    try
			    {
			    	 in = new FileReader("NIST.fa");
			         out1 = new FileWriter("NIST.fa.topics.ixml");
			         
			         BufferedReader br=new BufferedReader(in);
			         String line;
			         while ((line = br.readLine())!=null) 
			         {
			        	 	InstanceList testing1 = new InstanceList(instances.getPipe());
				     		testing1.addThruPipe(new Instance(line, null, "test instance", null));

				        	 double[] testProbs = inferencer.getSampledDistribution(testing1.get(0), 10, 1, 5);
				        	 
				        	 int j;
				        	 String output="<seg topic=\"";
				 			 for (j=0;j<numTopics;j++) 
				 				 output=output+Integer.toString(j)+" "+String.format("%.5f", testProbs[j])+" ";
				 			 output=output+("\">"+line+"</seg>\n");
				 			 out1.write(output);
			         }
			    	
			    }
			    finally
			    {
			    	if (in != null) 
			         {
			            in.close();
			         }
			         if (out1 != null)
			         {
			            out1.close();
			         }
			    }
			    //***********************************************************************

	}

}
