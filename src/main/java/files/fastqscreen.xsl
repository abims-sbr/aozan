<?xml version="1.0"?>
<!DOCTYPE html>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" 
doctype-system="about:legacy-compat"/>

<xsl:template match="/">
<xsl:decimal-format name="aozan" decimal-separator="." grouping-separator=" "/>
<xsl:decimal-format name="thousand" decimal-separator="." grouping-separator=" "/>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>fastqscreen <xsl:value-of select="/ReportFastqScreen/sampleName"/></title>
  <style type="text/css">
    
    #genomeSample{
    	color:#9B1319;
    	font-style : bold;
    }
    
    tr:hover {
      z-index:2;
      box-shadow:0 0 12px rgba(0, 0, 0, 1);
      background:#F6F6B4;
    }
    
    td {
      text-align: center;
      width: 100px;
      border: 1px solid black;
      
      padding-left:3px;
      padding-right:3px;
      padding-top:1px;
      padding-bottom:1px;
      position : static;
      background-clip: padding-box;
    }
    th {
       background-color:#E7EAF1;
       color:black;
       border: thin solid black;
       border-bottom-width: 2px;
       width: 50px;
       font-size: 100%;
    }
    table {
       border: medium solid #000000;
       font-size: 95%;
       border-collapse:collapse;
    }
    img {
  		width: 150px;
  	}
    body{
       font-family: sans-serif;   
       font-size: 80%;
       margin:0;
    }
    h1{
       color: #234CA5;
       font-style : italic;
       font-size : 20px;
    }
    h2{}
    h3{
       color:black;
       font-style : italic;
    }
    div.header {
        background-color: #C1C4CA;
        border:0;
        margin:0;
        padding: 0.5em;
        font-size: 175%;
        font-weight: bold;
        width:100%;
        height: 2em;
        position: fixed;
        vertical-align: middle;
        z-index:2;
        
    }
    #header_title {
        display:inline-block;
        float:left;
        clear:left;
    }
    div.report {
	    display:block;
	    position:absolute;
	    width:100%;
	    top:6em;
	    bottom:5px;
	    left:0;
	    right:0;
	    padding:0 0 0 1em;
	    background-color: white;
  	}
	div.footer {
	    background-color: #C1C4CA;
	    border:0;
	    margin:0;
	    padding:0.5em;
	    height: 1.3em;
	    overflow:hidden;
	    font-size: 100%;
	    font-weight: bold;
	    position:fixed;
	    bottom:0;
	    width:100%;
	    z-index:2;
   }
  </style>
</head>

<body>
  <div class="header">
  	<div id="header_title">
		<img src="http://tools.genomique.biologie.ens.fr/aozan/images/logo_aozan_qc.png" alt="Aozan"/>
		Detection contamination report
	</div>   
  </div>	
  
  <div class="report">
  <ul>
  	<li><b>Sequencer Type: </b> <xsl:value-of select="/ReportFastqScreen/SequencerType"/></li>
    <li><b>Run Id: </b> <xsl:value-of select="/ReportFastqScreen/RunId"/></li>
    <li><b>Flow cell: </b> <xsl:value-of select="/ReportFastqScreen/FlowcellId"/></li>
    <li><b>Run started: </b> <xsl:value-of select="/ReportFastqScreen/RunDate"/></li>
    <li><b>Instrument S/N: </b> <xsl:value-of select="/ReportFastqScreen/InstrumentSN"/></li>
    <li><b>Instrument run number: </b> <xsl:value-of select="/ReportFastqScreen/InstrumentRunNumber"/></li>
    <li><b>Generated by: </b> <xsl:value-of select="/ReportFastqScreen/GeneratorName"/> version <xsl:value-of select="/ReportFastqScreen/GeneratorVersion"/>
    	(revision <xsl:value-of select="/ReportFastqScreen/GeneratorRevision"/>)</li>
    <li><b>Creation date: </b> <xsl:value-of select="/ReportFastqScreen/ReportDate"/></li>
    </ul><ul>
    <li><b>Project : </b> <xsl:value-of select="/ReportFastqScreen/projectName"/></li>
    <li><b>Sample : </b> <xsl:value-of select="/ReportFastqScreen/sampleName"/></li>
    <li><b>Description : </b> <xsl:value-of select="/ReportFastqScreen/descriptionSample"/></li>
    <li><b>Genome sample : </b> <xsl:value-of select="/ReportFastqScreen/genomeSample"/></li>
  </ul>

  <table>
  	<xsl:variable name="genomeSample" select="/ReportFastqScreen/genomeSample"></xsl:variable>
  	
    <tr>
      <xsl:for-each select="/ReportFastqScreen/Report/Columns/Column">
        <th><xsl:value-of select="@name"/></th>
      </xsl:for-each>
    </tr>
   
   	<xsl:for-each select="/ReportFastqScreen/Report/Genomes/Genome">
	  <xsl:choose>
   	  <xsl:when test="boolean(@name=$genomeSample)">
		   <tr id="genomeSample">
		   	  <td><xsl:value-of select="@name"/></td>
		      <xsl:for-each select="Value">
		       	 <td><xsl:value-of select="format-number(.,'#0.00','aozan')"/> %</td>
		      </xsl:for-each>
		  	</tr>
   		</xsl:when>
   		<xsl:otherwise>
		   <tr>
		      <td><xsl:value-of select="@name"/></td>
		      <xsl:for-each select="Value">
		         <td><xsl:value-of select="format-number(.,'#0.00','aozan')"/> %</td>
		      </xsl:for-each>
		   </tr>
   		</xsl:otherwise>
   	</xsl:choose>
   
   </xsl:for-each>
   </table>
   
   <ul>
     <li><xsl:value-of select="/ReportFastqScreen/Report/ReadsUnmapped/@name"/> : 
     	<xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsUnmapped,'#0.00','aozan')"/> %</li>
     <li><xsl:value-of select="/ReportFastqScreen/Report/ReadsMappedOneGenome/@name"/> : 
     	<xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMappedOneGenome,'#0.00','aozan')"/> %</li>
     <li><xsl:value-of select="/ReportFastqScreen/Report/ReadsMappedExceptGenomeSample/@name"/> : 
     	<xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMappedExceptGenomeSample,'#0.00','aozan')"/> %</li>
   
     <li><xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMapped,'# ##0','thousand')"/>&#160;   
       <xsl:value-of select="/ReportFastqScreen/Report/ReadsMapped/@name"/>  / 
       <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsProcessed,'# ##0','thousand')"/>&#160;
       <xsl:value-of select="/ReportFastqScreen/Report/ReadsProcessed/@name"/>  </li>
   </ul>

   <p><a href="http://tools.genomique.biologie.ens.fr/aozan/qc-samples-tests.html#contamination" target="_blank">Contamination detection detail report</a></p>
   <p>-</p>
   </div>
   
	<div class="footer">
	Generated by 
	<xsl:element name="a">
		<xsl:attribute name="href"><xsl:value-of select="/ReportFastqScreen/GeneratorWebsite"/></xsl:attribute>Aozan</xsl:element>
	(version <xsl:value-of select="/ReportFastqScreen/GeneratorVersion"/>)
	
</div>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
