$(function() {

	function twitterFeed() {
		var tweetIndex = 1,
			$items = $('.jta-tweet-list-item'),
		    tweetNum = $items.length - 1;
		setInterval(function() {
			$items.hide().eq(tweetIndex).fadeIn();
			if (tweetIndex < tweetNum) {
				tweetIndex++;
			} else {
				tweetIndex = 0;
			}
		}, 10000);
	}
	
	twitterFeed();
});
