$(function() {

	function twitterFeed() {
		var tweetIndex = 1;
		var tweetNum = $('.az-twitterFeed .columns .jta-tweet-list .jta-tweet-list-item').length - 1;
		setInterval(function() {
			$('.az-twitterFeed .columns .jta-tweet-list .jta-tweet-list-item')
					.hide();
			$('.az-twitterFeed .columns .jta-tweet-list .jta-tweet-list-item')
					.eq(tweetIndex).fadeIn();
			if (tweetIndex < tweetNum) {
				tweetIndex++;
			} else {
				tweetIndex = 0;
			}
		}, 10000);
	}
	
	$(document).twitterFeed();
});
