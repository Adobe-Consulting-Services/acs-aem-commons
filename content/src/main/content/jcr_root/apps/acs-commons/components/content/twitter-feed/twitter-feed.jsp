<%@ page
	import="com.adobe.granite.xss.XSSAPI,
			com.adobe.acs.commons.twitter.util.TwitterUtil,
    			 com.day.cq.wcm.api.WCMMode"%>
<%@include file="/libs/foundation/global.jsp"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<cq:includeClientLib css="acs-commons.twitter.feed"/>
<cq:includeClientLib js="acs-commons.twitter.feed"/>

<%
	String[] tweets = properties.get("tweets", new String[0]);
	String[] filterWords = properties.get("filterWords", new String[0]);
	int noOfTweets = properties.get("noOfTweets", -1);

	tweets = TwitterUtil.filterTwitterFeeds(tweets, filterWords,
			noOfTweets);
	request.setAttribute("tweets", tweets);

	boolean isEdit = WCMMode.fromRequest(request) == WCMMode.EDIT;
%>


<c:set var="isEdit" value="<%=isEdit%>" />


<div class="az-twitterFeed row">
	<section class="small-12 columns">
		<section class="small-12 columns">
			<hr>
			<img src="/etc/designs/acs-commons/images/twitter-bird.png" alt="Twitter Bird" />
			<ul class="jta-tweet-list">	
				<c:choose>
					<c:when test="${fn:length(tweets) gt 0}">
						<c:forEach var="tweet" items="${tweets}">
							<li class="jta-tweet-list-item"><%=xssAPI.filterHTML((String) pageContext.getAttribute("tweet"))%></li>
						</c:forEach>
					</c:when>
					<c:when test="${isEdit}">
						<c:out value="The Twitter timeline for user:'${properties.username}' hasn't been fetched yet."/>
					</c:when>
				</c:choose>
			</ul>
		</section>
	</section>
</div>

