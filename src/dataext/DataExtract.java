package dataext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataExtract 
{
	private String dirReadPath="Collection/Collection/", ROOT_TAGNAME="DOC", FILE_START_NAME="cranfield";
	private HashMap<String, Integer> wMap=new HashMap<String, Integer>(), sMap=new HashMap<String, Integer>();
	private static int NUM_SORTED_SIZE=30;
	private Stemmer pStemmer=new Stemmer();		
	
	void printResults(HashMap<String, Integer> map, int numFiles, boolean pWordInfo)
	{
		Iterator<String> keyIterator=map.keySet().iterator();
		int numUniqueWords=map.keySet().size(), numWords=0, numSingularFreq=0;
		
		List<Word> sList = new ArrayList<Word>() {
			
			private static final long serialVersionUID = 1L;
			
			Comparator<Word> comp = new Comparator<Word>() {
		    	
		        public int compare(Word w1, Word w2) {
		        	
		        	if(w1.frequency.equals(w2.frequency))
		        		return w1.wStr.compareTo(w2.wStr);
		        	else
		        		return -1*w1.frequency.compareTo(w2.frequency);
		        }
		    };			
			
		    public boolean add(Word nWord) {
		    	
		        int index = Collections.binarySearch(this, nWord, comp);
		        
		        if (index < 0) 
		        	index = ~index;
		        
		        if(index<NUM_SORTED_SIZE)
		        {
	        		super.add(index, nWord);
	        		
	        		if(this.size()==(NUM_SORTED_SIZE+1))
	        			this.remove(NUM_SORTED_SIZE);
		        }
		        
		        return true;
		    }
		    
		};		
		
		while(keyIterator.hasNext())
		{
			String key=keyIterator.next();
			int numInstances=map.get(key); 
			
			numWords+=numInstances;
			
			if(numInstances==1)
				numSingularFreq++;
			
			Word word=new Word(key, numInstances);
			sList.add(word);			
		}
		
		if(pWordInfo)
			printWordInfo(numWords, numUniqueWords, numSingularFreq, sList, (int)(numUniqueWords/numFiles));
		else
			printStemInfo(numUniqueWords, numSingularFreq, sList, (int)(numUniqueWords/numFiles));
					
	}
		
	String processWord(String wStr)
	{
	    String tempStr = wStr.replaceAll("[^\\w]", "");
	    
	    if(isNumeric(tempStr))
	    {		    			    	
	    	tempStr=wStr.replaceAll("^[\\p{Punct}&&[^-]]+", "");	    	
	    	return tempStr.replaceAll("[\\p{Punct}]+$", "");
	    }
	    else if(isAbbreviation(tempStr))
	    	return tempStr;
	    else
	    	return tempStr.toLowerCase();		
	}
	
	void processText(String pStr)
	{
		String[] words = pStr.split("\\s+");		 
		
		for (int i = 0; i < words.length; i++)
		{
			words[i]=processWord(words[i]);
			
			if(words[i].length()>0)
			{			
				//Case sensitive comparison
				//Why?
				//U.S. will transform into US which shouldn't match up against "us" 			
				if(wMap.containsKey(words[i]))
					wMap.put( words[i], wMap.get(words[i])+1 );
				else
					wMap.put( words[i], 1);
				
				//Note that related words map to the same stem
				
				pStemmer.add(words[i].toCharArray(), words[i].length());				
				pStemmer.stem();
								
				words[i]=pStemmer.toString();			
											
				if(sMap.containsKey(words[i]))
					sMap.put( words[i], sMap.get(words[i])+1 );
				else
					sMap.put( words[i], 1);				
			}
			
		}
	
	}	
			
	String getText(NodeList nList)
	{
		String retStr="";
		
		for (int temp = 0; temp < nList.getLength(); temp++)
		{
			Node node = nList.item(temp);
			
			if (node.getNodeType() == Node.ELEMENT_NODE)
				retStr+=node.getTextContent();							
		}		
		
		return retStr;
	}
	
	String parseFile(String fName)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
	   
			Document document = builder.parse(new File(fName));
			document.getDocumentElement().normalize();
	   
			Element root = document.getDocumentElement();
			
			if(root.getNodeName().equals(ROOT_TAGNAME))		
				return getText( document.getElementsByTagName( root.getNodeName() ) );
			else
				return "";
	   
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return "";
		}
		
	}	
	
	void parseFiles()
	{
		File folder = new File(dirReadPath);
		File[] listOfFiles = folder.listFiles();

	    for (int i = 0; i < listOfFiles.length; i++) 
	    {
	    	if (listOfFiles[i].isFile() && listOfFiles[i].getName().startsWith(FILE_START_NAME)) 
	    		processText( parseFile(dirReadPath+listOfFiles[i].getName()) );	      
	    }	
	    
	    printResults(wMap, listOfFiles.length, true);
	    printResults(sMap, listOfFiles.length, false);
	}
			
	DataExtract(String dPath)
	{
		if(dPath.charAt(dPath.length()-1)=='/')
			dirReadPath=dPath;
		else
			dirReadPath=dPath+"/";
		
		parseFiles();
	}
	
	void printList(List<Word> sList)
	{		
		for(int i=0;i<sList.size();i++)
			System.out.println(sList.get(i).wStr+" -> "+sList.get(i).frequency);
	}
	
	void printWordInfo(int numWords, int numUniqueWords, int numSingularFreq, List<Word> sList, int avgUniWords)
	{
		System.out.println("Number of Tokens in Collection: "+numWords);
		System.out.println("Number of Unique Words in Collection: "+numUniqueWords);
		System.out.println("Number of Words that occur only once in the the Collection: "+numSingularFreq);
		
		System.out.println("30 Most frequent Words in the Collection:\n");
		printList(sList);	
		
		System.out.println("\nAverage number of Unique Words per Document: "+avgUniWords);		
	}
	
	void printStemInfo(int numUniqueStems, int numSingularFreq, List<Word> sList, int avgUniStems)
	{
		System.out.println("Number of Distinct Stems: "+numUniqueStems);
		System.out.println("Number of Stems that occur only once: "+numSingularFreq);
		
		System.out.println("30 Most frequent Stems in the Collection:\n");
		printList(sList);	
		
		System.out.println("\nAverage number of Unique Stems per Document: "+avgUniStems);		
	}	
	
	boolean isNumeric(String str)
	{
	    for (char c : str.toCharArray())
	    {
	        if (!Character.isDigit(c)) 
	        	return false;
	    }
	    return true;
	}
	
	boolean isAbbreviation(String str)
	{
		//All the characters are upper-case
		
		for(int i=0;i<str.length();i++)
		{
			if(Character.isLowerCase(str.charAt(i)))
				return false;
		}
		
		return true;
	}	

	public static void main (String[] args) 
	{		
		if(args.length>0)
			new DataExtract(args[0]);
		else
			System.out.println("Please enter the path to find all files associated with the Collection");
	}
	
	public class Word
	{
		String wStr;
		Integer frequency;
		
		Word(String word, Integer frequency)
		{
			this.wStr=word;
			this.frequency=frequency;
		}
		
	}	

}