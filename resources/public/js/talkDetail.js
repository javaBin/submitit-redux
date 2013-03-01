$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}

function TalkDetailCtrl($scope,$http) {
	var givenId = $.urlParam("talkid");
	var jsonurl = "talkJson?talkid=" + givenId;

	$scope.talkData = {}

	$http({method: 'GET', url: jsonurl}).
  		success(function(data, status, headers, config) {
  			$scope.talkData = data;
  		}).
  		error(function(data, status, headers, config) {
		    console.log("some error occured");
	  	});

	
	
}