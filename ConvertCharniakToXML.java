import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;
import java.util.ArrayList;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.sax.*;

public class ConvertCharniakToXML  
{
  final File maindir = new File("PATH_TO_DIRECTORY_WITH_TXT_FILES");//txt file format should be like the one given
  final String output = "PATH_TO_OUTPUT_DIRECTORY";
  File outpath;
  File[] dirs, files;
  BufferedReader in;
  StreamResult out;
  StringBuffer strb = new StringBuffer();
  Vector tags;
  TransformerHandler th;
  AttributesImpl atts;

  public ConvertCharniakToXML()
  {
     dirs = maindir.listFiles();
     for(int i=0;i<dirs.length;i++)
     {
        if(dirs[i].isDirectory())
        {
            outpath = new File(output+dirs[i].toString().split("/")[5]);
            files = dirs[i].listFiles();
            if(!outpath.exists())
                outpath.mkdirs();
            for(int j=0;j<files.length;j++)
            {
                if(! files[j].toString().startsWith("."))
                   start(files[j]);
            }
        }
     } 
     //If you want to try it for one file, comment the above for-loop and uncomment the below line
     //start(new File("no1.txt"));
   }
    
  public void start (File f)
  {
    try{
        strb.delete(0,strb.length());
        if(!(f.getName().startsWith("."))) //ignore hidden or temp files
        {
           in = new BufferedReader(new FileReader(f));
                                          //f.getName().replace("txt","xml") - for one file
           out = new StreamResult(new File(outpath+"/"+f.getName().replace("txt","xml")));
           initXML();
           String str;
           while ((str = in.readLine()) != null)
               strb.append(str.trim());
    
           process(strb);
           in.close();
                           //f.getName().replace("txt","xml") - for one file
           closeXML(new File(outpath+"/"+f.getName().replace("txt","xml")));
        }
    }catch (Exception e) { e.printStackTrace(); }
  }

  public void initXML() throws ParserConfigurationException,TransformerConfigurationException, SAXException
  {  
    SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
    th = tf.newTransformerHandler();
    Transformer serializer = th.getTransformer();
    //serializer.setOutputProperty(OutputKeys.ENCODING,"ISO-8859-1");
    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");
    serializer.setOutputProperty(OutputKeys.INDENT,"yes");
    th.setResult(out);
    th.startDocument();
    atts = new AttributesImpl();
    th.startElement("","","S1",atts);
  }

  private void process (StringBuffer s) throws SAXException
  {
    String [] elements = s.toString().trim().split("\\(");
    char[] text = {' '};
    tags = new Vector();
    ArrayList symbols = new ArrayList();
    String endtag = "";
    atts.clear();
    tags.add("S1");
    symbols.add(","); symbols.add("\""); symbols.add(":"); symbols.add("'"); symbols.add("``"); symbols.add(".");
    symbols.add("''"); symbols.add("-RRB-"); symbols.add("-LRB-"); symbols.add("PRP$"); symbols.add("WP$");
    for(int i=2;i<elements.length;i++)
    {
       if(elements[i].trim().split(" ").length>1)
       {
           if(!symbols.contains(elements[i].split(" ")[0].trim()))
           {
                th.startElement("","","w pos=\""+elements[i].split(" ")[0].trim()+"\"",atts);
                th.characters(elements[i].split(" ")[1].trim().toCharArray(),0,elements[i].split(" ")[1].trim().length()-1);
           }
           else if(symbols.contains(elements[i].split(" ")[0].trim()))
           {
               if(elements[i].split(" ")[0].trim().endsWith("$"))
               {
                    endtag = "dollar";
                    th.startElement("","","w pos=\""+elements[i].split(" ")[0].replace("$","")+"\"",atts);
                    th.characters(elements[i].split(" ")[1].trim().toCharArray(),0,elements[i].split(" ")[1].trim().length()-1);
                }
               else
               {
                   endtag = "symbol";
                   th.startElement("","","symbol",atts);
                   th.characters(elements[i].split(" ")[1].trim().toCharArray(),0,elements[i].split(" ")[1].trim().length()-1);
                }
           }
       }
       else
       {
           tags.add(elements[i].trim());
           th.startElement("","",elements[i].trim(),atts);
        }
           
       if(elements[i].trim().endsWith(")"))
       {
           if(endtag.equals("symbol"))
           {
              th.endElement("","",endtag);
              endtag="";
              text = elements[i].split(" ")[1].trim().substring(0,elements[i].split(" ")[1].trim().length()-1).toCharArray();
              if(text[text.length-1] == ')')
                 checkTags(text,text.length-1);
           }
           else if(endtag.equals("dollar"))
           {
              th.endElement("","","w");
              endtag="";
              text = elements[i].split(" ")[1].trim().substring(0,elements[i].split(" ")[1].trim().length()-1).toCharArray();
              if(text[text.length-1] == ')')
                 checkTags(text,text.length-1);
            }
           else
           {
              th.endElement("","","w");
              text = elements[i].split(" ")[1].trim().substring(0,elements[i].split(" ")[1].trim().length()-1).toCharArray();
              if(text[text.length-1] == ')')
                 checkTags(text,text.length-1);
           }               
        }
     }
     symbols.clear();
  }
  
  private void checkTags(char[] tex,int size) throws SAXException 
  {
      String endtag = (String)tags.get(tags.size()-1);
      th.endElement("","",endtag);
      //System.out.println("old tags: "+tags + " [size= "+tags.size()+"]");
      tags.remove(tags.size()-1);
      //System.out.println("remaining tag: "+tags+ " [size= "+tags.size()+"]");
      size = size - 1; 
      if(tex[size]==')')
        checkTags(tex,size); 
      else
        return;
    }
    
  public void closeXML(File f) throws SAXException
  {
    th.endDocument();
    strb.delete(0,strb.length());
    processXML(f);
    //System.exit(0);
  }
  
  public void processXML(File file)
  {
      BufferedReader r;
      BufferedWriter w;
      StringBuffer strb = new StringBuffer();
      try{
           r = new BufferedReader(new FileReader(file));
           String line = r.readLine();
           strb.append(line + "\n<TEXT>\n"); 
           while(line!=null)
           {
               if(!line.startsWith("<?xml"))
                    strb.append(line.replaceAll("\\)","")+"\n");
               line = r.readLine();
           }
           strb.append("</TEXT>");
           w = new BufferedWriter(new FileWriter(file));
           w.write(strb.toString());
           w.close();
           strb.delete(0,strb.length());
        }catch(IOException ex){System.out.println(ex.getMessage());}
    }
  
  public static void main(String args[])
  {
      new ConvertCharniakToXML();
    }
}