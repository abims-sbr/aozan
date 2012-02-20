<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="1.0">

<xsl:template match="/">
<xsl:decimal-format name="aozan" decimal-separator="." grouping-separator=" "/>

<html>
<head>
  <title><xsl:value-of select="/QCReport/RunId"/> run quality report</title>
  <style TYPE="text/css">
    td {
      text-align: center;
    }

    .score-1 {
    }
    .score0 {
      background: red;
    }
    .score1 {
      background: red;
    }
    .score2 {
      background: red;
    }
    .score3 {
      background: red;
    }
    .score4 {
      background: red;
    }
    .score5 {
      background: green;
    }
    .score6 {
      background: green;
    }
    .score7 {
      background: green;
    }
    .score8 {
      background: green;
    }
    .score9 {
      background: green;
    }
  </style>
</head>
<body>

  <h1><xsl:value-of select="/QCReport/RunId"/> Quality report</h1> 

  <ul>
    <li><b>Run Id: </b> <xsl:value-of select="/QCReport/RunId"/></li>
    <li><b>Flow cell: </b> <xsl:value-of select="/QCReport/FlowcellId"/></li>
    <li><b>Date: </b> <xsl:value-of select="/QCReport/RunDate"/></li>
    <li><b>Instrument S/N: </b> <xsl:value-of select="/QCReport/InstrumentSN"/></li>
    <li><b>Instrument run number: </b> <xsl:value-of select="/QCReport/InstrumentRunNumber"/></li>
    <li><b>Generated by: </b> <xsl:value-of select="/QCReport/GeneratorName"/> version <xsl:value-of select="/QCReport/GeneratorVersion"/></li>
    <li><b>Creation date: </b> <xsl:value-of select="/QCReport/ReportDate"/></li>
  </ul>

  <h2>Lanes Quality report</h2>

  <xsl:for-each select="/QCReport/ReadsReport/Reads/Read">
    <h3>Read <xsl:value-of select="@number"/> (<xsl:value-of select="@cycles"/> cycles<xsl:if test="@indexed='true'">, index</xsl:if>)</h3>
    <table border="1">
    <tr>
      <th>Lane</th>
      <xsl:for-each select="/QCReport/ReadsReport/Columns/Column">
        <th><xsl:value-of select="."/><xsl:if test="@unit!=''"> (<xsl:value-of select="@unit"/>)</xsl:if></th>
      </xsl:for-each>
    </tr>
    <xsl:for-each select="Lane">
      <tr>
       <td><xsl:value-of select="@number"/></td>
       <xsl:for-each select="Test">
         <td class="score{@score}">
           <xsl:if test="@type='int'"><xsl:value-of select="format-number(.,'### ### ### ### ###','aozan')"/></xsl:if>
           <xsl:if test="@type='float'"><xsl:value-of select="format-number(.,'### ### ### ##0.00','aozan')"/></xsl:if>
           <xsl:if test="@type='percent'"><xsl:value-of select="format-number(.,'#0.00%','aozan')"/></xsl:if>
           <xsl:if test="@type='string'"><xsl:value-of select="."/></xsl:if>
         </td>
       </xsl:for-each>
     </tr>
    </xsl:for-each>

   </table>
  </xsl:for-each>

  <h2>Samples Quality report</h2>

  <xsl:for-each select="/QCReport/SamplesReport/Reads/Read">
    <h3>Read <xsl:value-of select="@number"/></h3>
    <table border="1">
    <tr>
      <th>Lane</th>
      <th>Sample name</th>
      <th>Description</th>
      <th>Index</th>
      <xsl:for-each select="/QCReport/SamplesReport/Columns/Column">
        <th><xsl:value-of select="."/><xsl:if test="@unit!=''"> (<xsl:value-of select="@unit"/>)</xsl:if></th>
      </xsl:for-each>
    </tr>
    <xsl:for-each select="Sample">
      <tr>
       <td><xsl:value-of select="@lane"/></td>
       <td><xsl:value-of select="@name"/></td>
       <td><xsl:value-of select="@desc"/></td>
       <td><xsl:value-of select="@index"/></td>
       <xsl:for-each select="Test">
         <td class="score{@score}">
           <xsl:if test="@type='int'"><xsl:value-of select="format-number(.,'### ### ### ### ###','aozan')"/></xsl:if>
           <xsl:if test="@type='float'"><xsl:value-of select="format-number(.,'### ### ### ##0.00','aozan')"/></xsl:if>
           <xsl:if test="@type='percent'"><xsl:value-of select="format-number(.,'#0.00%','aozan')"/></xsl:if>
           <xsl:if test="@type='string'"><xsl:value-of select="."/></xsl:if>
         </td>
       </xsl:for-each>
     </tr>
    </xsl:for-each>

   </table>
  </xsl:for-each>

</body></html>
</xsl:template>
</xsl:stylesheet>