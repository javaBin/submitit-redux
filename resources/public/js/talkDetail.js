$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}

function TalkDetailCtrl($scope) {
	var givenId = $.urlParam("talkid");
	$scope.talkid=givenId;
}