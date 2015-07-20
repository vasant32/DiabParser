import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 */

/**
 * @author drashtti
 *
 */
public class readXMLFiles extends DefaultHandler {
	
	//total number of hgnc words
	private float hgncCount = 0;
	private float totalAbstracts =7416; 
	private String hgncId;
	private String diabId;
	private String tempVal;
	private String pmid;
	private int counter = 0;
	private int count = 1;
	private int counts = 1;
	//map to store the ontology id and pubmed id 
	private Map<String, String> terms;
	//map of ontID and count - to count no of times the word appeared in a document to calculate TF
	private Map <String,Float> localCount;
	//creating the output sheet
	private HSSFWorkbook wb;
	private HSSFWorkbook wb1;
	private Sheet s;
	private Sheet s3;

	
	public void readDirectory(File[] files){
		this.wb = new HSSFWorkbook();
		this.s = wb.createSheet();
		this.wb1 = new HSSFWorkbook();
		this.s3 = wb1.createSheet();
		this.terms = new HashMap<String, String>();
		
		for (File file : files){
			if (file.isDirectory()){
				System.out.println("Directory: " + file.getName());
				readDirectory(file.listFiles());//calls the same method again
			} else{
				System.out.println("File: " + file.getName());
				String id = file.getName().split("_")[0];
				String citation_count = file.getName().split("_")[1];
				String XMLpath = file.getAbsolutePath();
				parseXML(XMLpath , id, citation_count);
				calculatetf();
				counter ++;
			}
		}
		
		
	}
	
	public  void parseXML(String XMLpath, String id, String citationCount){
		 System.out.println("Parsing: " + XMLpath + " with pubmed id " + id + " and counter is " + counter);
		    pmid = id;
		    this.localCount = new HashMap<String, Float>();
		    SAXParserFactory spf = SAXParserFactory.newInstance();
		    boolean success = false;
		    int tries = 0;
		    Exception lastException = null;
//		    try {
		    	while (!success & tries < 1) {
		    		try {
		    			tries++;
			            SAXParser sp = spf.newSAXParser();
			            sp.parse(XMLpath, this);
			            success = true;
		    		}
		    		catch (Exception e) {
		    			System.err.println("Problem parsing " + XMLpath + " (" + e.getMessage() + ").  Retries remaining = " + (1-tries));
		    			lastException = e;
		    		}
		    	}
		    	if (!success) {
		    		lastException.printStackTrace();
		    		throw new RuntimeException("Failed to parse " +XMLpath + " after 5 retries", lastException);
		    	}
			
	}
	
	
	
	//Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	//reset
	tempVal = "";
	if (qName.equalsIgnoreCase("z:hgnc")){
		hgncId = attributes.getValue("ids");
		}	
	if (qName.equalsIgnoreCase("z:DIAB")){
		diabId = attributes.getValue("ids");
		}
    }
    
    
    public void characters(char[] ch, int start, int length) throws SAXException {
	tempVal = new String(ch,start,length);
	//System.out.println(ch);
    }
    
    /*
     * (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     *
     */
    
    public void endElement(String uri, String localName,
			   String qName) throws SAXException {

		 
	if(qName.equalsIgnoreCase("z:hgnc")) {
		//System.out.println(tempVal + " with id " + hgncId);
		if((terms.containsKey(hgncId) && (!pmid.equalsIgnoreCase(terms.get(hgncId)))) || !terms.containsKey(hgncId) ){
			//diseaseCount = diseaseCount++;
			System.out.println("came across a new term " + hgncId);
			terms.put(hgncId, pmid);
			addData(tempVal, "HGNC");
			this.hgncCount = hgncCount + 1 ;
			localCount.put(hgncId, 1f);
		}
		else {
			terms.put(hgncId, pmid);
			this.hgncCount= hgncCount + 1;
			System.out.println("come across the same term " + hgncId);
			float temp = localCount.get(hgncId)+1 ;
			localCount.put(hgncId, temp);
			}
		}
	}
	
	
	
	
	
	private void addData(String Value, String ont) {
		// Define a few rows
   	   for(int rownum = count; rownum <= count; rownum++) {
   		   Row r1 = this.s.createRow(rownum); 
   		// System.out.println("in add data sheet s = " + s);   
		   for(int cellnum = 0; cellnum < 1; cellnum ++) {
			   Cell c1 = r1.createCell(cellnum);
			   Cell c2 = r1.createCell(cellnum+1);
			   Cell c3 = r1.createCell(cellnum+2);
			   Cell c4 = r1.createCell(cellnum+3);
			   Cell c5 = r1.createCell(cellnum+4);
			   Cell c6 = r1.createCell(cellnum+5);
			  
	   
			   c1.setCellValue("Type 2 diabetes");
			   c2.setCellValue("is_associated_with_gene");
			   c3.setCellValue(Value);
			   c4.setCellValue(ont);
			   c5.setCellValue(hgncId);
			   c6.setCellValue(this.pmid);
			
		   		}   		   
	   		}
	  this.count = count+1;   		
	}

	public void calculatetf(){
		System.out.println("calculating tf ...");
		
		for (String key : localCount.keySet()){
			String tempOID = key;
    		float numOfOccurrences = localCount.get(key);    		
    		float totalTermsInDocument = hgncCount;
    		float tf = numOfOccurrences / totalTermsInDocument;
    		TermFrequencyData tdf = new TermFrequencyData(pmid,tempOID,tf);
    		addTfData(tdf);
			
		}
		
		
	}
	
	
private Set<TermFrequencyData> tdfs = new HashSet<TermFrequencyData>();
    
    public void addTfData(TermFrequencyData tdf) {
    	tdfs.add(tdf);
    }
    
    
    public void addTfdata(String file) throws InvalidFormatException, FileNotFoundException, IOException{
 	   System.out.println("Starting to add term frequency data. Size of Tdf set:" + tdfs.size());
 	   Workbook work = WorkbookFactory.create(new FileInputStream(file));
 	   Sheet ws = work.getSheetAt(0);
 	   int rownum = ws.getLastRowNum();
 	   for(int num = 1; num <= rownum; num++){
 		   Row rr = ws.getRow(num);
 		   Cell pmid = rr.getCell(5);
 		   String pid = pmid.toString();
 		   Cell ontid = rr.getCell(4);
 		   String oid = ontid.toString();
 		   for(TermFrequencyData temp :tdfs){
 			   String tempPid = temp.getPubmedId();
 			   String tempOid = temp.getOntoID();
 			   float temptf = temp.getTermFreq();
 			   if (pid.equalsIgnoreCase(tempPid) && oid.equalsIgnoreCase(tempOid)){
 				   Cell x = rr.createCell(7);
 				   x.setCellValue(temptf);
 			   }
 		   }
 	   }
 	   FileOutputStream fout = new FileOutputStream(file);
 	   work.write(fout);
 	   fout.close();
    }
    
    
    
    public void postProcess(String filen) throws InvalidFormatException, FileNotFoundException, IOException{
 	   System.out.println("Start of post processing...");
 	   //map for onto id and abstract count
 	   Map<String,Integer> totalCount = new HashMap<String,Integer>();
 	   //map of onto id and its corresponding phenotype name
 	   Map<String,String> names = new HashMap<String, String>();
 	   //map of onto id and adding up its tf
 	   Map<String,Float> frequency = new HashMap<String, Float>();
 	   Workbook work = WorkbookFactory.create(new FileInputStream(filen));
 	   Sheet ws = work.getSheetAt(0);
 	   int rownum = ws.getLastRowNum();
 	   System.out.println("the number of rows in the sheet are :" + rownum);
 	   //Row r = ws.getRow(0);
 	   //int columnNum = r.getLastCellNum();
 	   //System.out.println(" the number of columns in the first row are :" + columnNum);
 	   
 	   for ( int num =1; num<= rownum; num++){
 		   Row rr = ws.getRow(num);
 		   Cell phenos = rr.getCell(2);
 		   String pheno = phenos.toString();
 		   Cell ids = rr.getCell(4);
 		   String id = ids.toString();
 		   Cell tf = rr.getCell(7);
 		   String tfs = tf.toString();
 		   float tfcount = Float.parseFloat(tfs);
 		 // System.out.println("the citation count is" + cite );
 		   if (!names.containsKey(id)){
 		   names.put(id, pheno);
 		   totalCount.put(id, 1);
 		   frequency.put(id, tfcount);
 		 
 		   }
 		   else {
 			   int abstractcount = totalCount.get(id) + 1;
 			   totalCount.put(id, abstractcount);
 			   float tfcounter = frequency.get(id) + tfcount;
 			   frequency.put(id, tfcounter);
 			  }
 		   
 	   }//end of for loop
 	   
 	   for (String x : names.keySet()){
 		  String pheno = names.get(x);
 		  int abCount = totalCount.get(x);
 		   float meantf = frequency.get(x) / abCount;
 		   float idf = (float) Math.log10(totalAbstracts/abCount);
 		   float tfidf = meantf * idf; 
 		   addPostProcessData(x,pheno,abCount,meantf,tfidf);
 		   
 	   }
 	  createPostProcessFile(); 
 	 }

    
    
    public void addPostProcessData(String id, String pheno, int abCount, float meantf, float tfidf){
 	// Define a few rows
   	   for(int rownum = counts; rownum <= counts; rownum++) {
   		   Row r1 = this.s3.createRow(rownum); 
   		// System.out.println("in add data sheet s = " + s);   
 	   for(int cellnum = 0; cellnum < 1; cellnum ++) {
 		   Cell c1 = r1.createCell(cellnum);
 		   Cell c2 = r1.createCell(cellnum+1);
 		   Cell c3 = r1.createCell(cellnum+2);
 		   Cell c4 = r1.createCell(cellnum+3);
 		   Cell c6 = r1.createCell(cellnum+5);
 		   Cell c7 = r1.createCell(cellnum+6);
 		  
 		   c1.setCellValue("Type 2 diabetes");
 		   c2.setCellValue(id);
 		   c3.setCellValue(pheno);
 		   c4.setCellValue(abCount);
 		   c6.setCellValue(meantf);
 		   c7.setCellValue(tfidf);
 		   		   
 	   		}   		   
    		}
   this.counts = counts+1; 
 	   
 	   
    }
    
    
    public void createPostProcessFile() throws IOException{
 	   FileOutputStream fo = new FileOutputStream("processedtype_2_diab_gene.xls");
 	   wb1.write(fo);
 	   fo.close();
 	   System.out.println("processedtype_2_diab_gene.xls file created");
 	   
    }
    
    public void saveToFile() throws IOException, InvalidFormatException{
   	 String filename = "type_2_diab_gene.xls"; // change the name as required
   	 FileOutputStream out = new FileOutputStream(filename);
   	 wb.write(out);
   	 out.close();
   	 System.out.println(filename + "File created");
   	 addTfdata(filename);
   	 postProcess(filename); 	 
      }

	public static void main(String[] args) throws InvalidFormatException, IOException {
		File[] files = new File("/home/drashtti/Desktop/ontologies/Diabetes-Onto/type_2_diabetesAbstractsCuratedMined").listFiles();
		readXMLFiles obj = new readXMLFiles();
		obj.readDirectory(files);
		obj.saveToFile();
		//obj.calculatetf();
		

	}

}
