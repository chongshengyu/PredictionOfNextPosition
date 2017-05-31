package com.yu.spider;

import java.util.Iterator;
import java.util.List;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;


//给定期刊 年卷期列表链接，和关键字数组，爬取得到在哪一卷期中出现过这个关键字，给出卷期和文章链接
public class simple {
	public static Document parse(URL url) throws DocumentException{
		SAXReader reader = new SAXReader();
		Document document = reader.read(url);
		return document;
	}
	
	public static ArrayList<String> getHrefList(Document doc){
		ArrayList<String> results = new ArrayList<String>();
		List list = doc.selectNodes("//div[@id='main']/ul/a/");
		for(Iterator iterator=list.iterator();iterator.hasNext();){
			Node node = (Node)iterator.next();
			String string = node.valueOf("@href");
			results.add(string);
		}
		return results;
	}
	
	public static void main(String[] args) throws MalformedURLException, DocumentException {
		String urlString = "http://dblp.uni-trier.de/db/journals/isci/";
		URL url = new URL(urlString);
		System.out.println(getHrefList(parse(url)).size());
	}
}
