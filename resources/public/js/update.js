"use strict";

$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}

$(function() {
    $('input,select').keypress(function(event) { return event.keyCode != 13; });
});

function get_gravatar(email) {

    // MD5 (Message-Digest Algorithm) by WebToolkit
    //

    var MD5=function(s){function L(k,d){return(k<<d)|(k>>>(32-d))}function K(G,k){var I,d,F,H,x;F=(G&2147483648);H=(k&2147483648);I=(G&1073741824);d=(k&1073741824);x=(G&1073741823)+(k&1073741823);if(I&d){return(x^2147483648^F^H)}if(I|d){if(x&1073741824){return(x^3221225472^F^H)}else{return(x^1073741824^F^H)}}else{return(x^F^H)}}function r(d,F,k){return(d&F)|((~d)&k)}function q(d,F,k){return(d&k)|(F&(~k))}function p(d,F,k){return(d^F^k)}function n(d,F,k){return(F^(d|(~k)))}function u(G,F,aa,Z,k,H,I){G=K(G,K(K(r(F,aa,Z),k),I));return K(L(G,H),F)}function f(G,F,aa,Z,k,H,I){G=K(G,K(K(q(F,aa,Z),k),I));return K(L(G,H),F)}function D(G,F,aa,Z,k,H,I){G=K(G,K(K(p(F,aa,Z),k),I));return K(L(G,H),F)}function t(G,F,aa,Z,k,H,I){G=K(G,K(K(n(F,aa,Z),k),I));return K(L(G,H),F)}function e(G){var Z;var F=G.length;var x=F+8;var k=(x-(x%64))/64;var I=(k+1)*16;var aa=Array(I-1);var d=0;var H=0;while(H<F){Z=(H-(H%4))/4;d=(H%4)*8;aa[Z]=(aa[Z]|(G.charCodeAt(H)<<d));H++}Z=(H-(H%4))/4;d=(H%4)*8;aa[Z]=aa[Z]|(128<<d);aa[I-2]=F<<3;aa[I-1]=F>>>29;return aa}function B(x){var k="",F="",G,d;for(d=0;d<=3;d++){G=(x>>>(d*8))&255;F="0"+G.toString(16);k=k+F.substr(F.length-2,2)}return k}function J(k){k=k.replace(/rn/g,"n");var d="";for(var F=0;F<k.length;F++){var x=k.charCodeAt(F);if(x<128){d+=String.fromCharCode(x)}else{if((x>127)&&(x<2048)){d+=String.fromCharCode((x>>6)|192);d+=String.fromCharCode((x&63)|128)}else{d+=String.fromCharCode((x>>12)|224);d+=String.fromCharCode(((x>>6)&63)|128);d+=String.fromCharCode((x&63)|128)}}}return d}var C=Array();var P,h,E,v,g,Y,X,W,V;var S=7,Q=12,N=17,M=22;var A=5,z=9,y=14,w=20;var o=4,m=11,l=16,j=23;var U=6,T=10,R=15,O=21;s=J(s);C=e(s);Y=1732584193;X=4023233417;W=2562383102;V=271733878;for(P=0;P<C.length;P+=16){h=Y;E=X;v=W;g=V;Y=u(Y,X,W,V,C[P+0],S,3614090360);V=u(V,Y,X,W,C[P+1],Q,3905402710);W=u(W,V,Y,X,C[P+2],N,606105819);X=u(X,W,V,Y,C[P+3],M,3250441966);Y=u(Y,X,W,V,C[P+4],S,4118548399);V=u(V,Y,X,W,C[P+5],Q,1200080426);W=u(W,V,Y,X,C[P+6],N,2821735955);X=u(X,W,V,Y,C[P+7],M,4249261313);Y=u(Y,X,W,V,C[P+8],S,1770035416);V=u(V,Y,X,W,C[P+9],Q,2336552879);W=u(W,V,Y,X,C[P+10],N,4294925233);X=u(X,W,V,Y,C[P+11],M,2304563134);Y=u(Y,X,W,V,C[P+12],S,1804603682);V=u(V,Y,X,W,C[P+13],Q,4254626195);W=u(W,V,Y,X,C[P+14],N,2792965006);X=u(X,W,V,Y,C[P+15],M,1236535329);Y=f(Y,X,W,V,C[P+1],A,4129170786);V=f(V,Y,X,W,C[P+6],z,3225465664);W=f(W,V,Y,X,C[P+11],y,643717713);X=f(X,W,V,Y,C[P+0],w,3921069994);Y=f(Y,X,W,V,C[P+5],A,3593408605);V=f(V,Y,X,W,C[P+10],z,38016083);W=f(W,V,Y,X,C[P+15],y,3634488961);X=f(X,W,V,Y,C[P+4],w,3889429448);Y=f(Y,X,W,V,C[P+9],A,568446438);V=f(V,Y,X,W,C[P+14],z,3275163606);W=f(W,V,Y,X,C[P+3],y,4107603335);X=f(X,W,V,Y,C[P+8],w,1163531501);Y=f(Y,X,W,V,C[P+13],A,2850285829);V=f(V,Y,X,W,C[P+2],z,4243563512);W=f(W,V,Y,X,C[P+7],y,1735328473);X=f(X,W,V,Y,C[P+12],w,2368359562);Y=D(Y,X,W,V,C[P+5],o,4294588738);V=D(V,Y,X,W,C[P+8],m,2272392833);W=D(W,V,Y,X,C[P+11],l,1839030562);X=D(X,W,V,Y,C[P+14],j,4259657740);Y=D(Y,X,W,V,C[P+1],o,2763975236);V=D(V,Y,X,W,C[P+4],m,1272893353);W=D(W,V,Y,X,C[P+7],l,4139469664);X=D(X,W,V,Y,C[P+10],j,3200236656);Y=D(Y,X,W,V,C[P+13],o,681279174);V=D(V,Y,X,W,C[P+0],m,3936430074);W=D(W,V,Y,X,C[P+3],l,3572445317);X=D(X,W,V,Y,C[P+6],j,76029189);Y=D(Y,X,W,V,C[P+9],o,3654602809);V=D(V,Y,X,W,C[P+12],m,3873151461);W=D(W,V,Y,X,C[P+15],l,530742520);X=D(X,W,V,Y,C[P+2],j,3299628645);Y=t(Y,X,W,V,C[P+0],U,4096336452);V=t(V,Y,X,W,C[P+7],T,1126891415);W=t(W,V,Y,X,C[P+14],R,2878612391);X=t(X,W,V,Y,C[P+5],O,4237533241);Y=t(Y,X,W,V,C[P+12],U,1700485571);V=t(V,Y,X,W,C[P+3],T,2399980690);W=t(W,V,Y,X,C[P+10],R,4293915773);X=t(X,W,V,Y,C[P+1],O,2240044497);Y=t(Y,X,W,V,C[P+8],U,1873313359);V=t(V,Y,X,W,C[P+15],T,4264355552);W=t(W,V,Y,X,C[P+6],R,2734768916);X=t(X,W,V,Y,C[P+13],O,1309151649);Y=t(Y,X,W,V,C[P+4],U,4149444226);V=t(V,Y,X,W,C[P+11],T,3174756917);W=t(W,V,Y,X,C[P+2],R,718787259);X=t(X,W,V,Y,C[P+9],O,3951481745);Y=K(Y,h);X=K(X,E);W=K(W,v);V=K(V,g)}var i=B(Y)+B(X)+B(W)+B(V);return i.toLowerCase()};


    return 'http://www.gravatar.com/avatar/' + MD5(email) + '.jpg?s=512&d=identicon';

}

function getDummySpeakerId() {
    //return "xuz";
    var dummySpeakerId;
    $.ajax({
        url: "newSpeakerId",
        async: false,
        cache: false,
        success: function(data) {
            dummySpeakerId=$.parseJSON(data);
        }
    });
    return dummySpeakerId.dummyId;
}

function readCaptchaFact() {
    var captchaFact;
    $.ajax({
        url: "loadCaptcha",
        async: false,
        cache: false,
        dataType: 'json',
        success: function(data, a, b) {
            captchaFact=data;
        }
    });
    return captchaFact.fact;
}

function pictureChanged(dummyKey,speakerKey) {
    angular.element("#mainElement").scope().newPicture(dummyKey,speakerKey);
}

angular.module('submititapp', [])
//    .config(function($sceProvider) {
//        $sceProvider.enabled(false);
//    })
    .controller('UpdateCtrl', ['$scope','$http', function($scope,$http) {

    $scope.showMain = true;
    $scope.showUserError = false;
    $scope.showResult = false;
    $scope.showResultSuccess = false;
    $scope.showResutFailure = false;
    $scope.showCapthcaError = false;
    $scope.showFailure = false;
    $scope.failureError = false;
    $scope.talkAddress = false;
    $scope.needPassword = false;
    $scope.showCapthca = true;
    $scope.showGeneralError = false;


    var talkid = $.urlParam("talkid");
    var captchaFact = readCaptchaFact();
    var typeDescription = {
        presentation: {
            text: "Presentations can have a length of 45 or 60 minutes. Including Q&A",
            lengths: [{id:45,text:"45 minutes"},{id:60,text:"60 minutes"}]
        },
        "lightning-talk": {
            text: "Lightning talks can be 10 or 20 minutes long. The time limit is strictly enforced",
            lengths: [{id:10,text:"10 minutes"},{id:20,text:"20 minutes"}]
        },
        workshop: {
            text: "Workshops last 2, 4 or 8 hours (120, 240 or 480 minutes)",
            lengths: [{id:120,text:"120 minutes"},{id:240,text:"240 minutes"},{id:480,text:"480 minutes"}]
        }
    }
    $scope.prestypeChanged = function(presentationType) {
        $scope.lengthDescription = typeDescription[presentationType].text;
        $scope.talkLengths = typeDescription[presentationType].lengths;
    };

    $scope.addATag = function() {
        var newTag = $("#tagInput").val();
        if (!newTag || newTag.trim().length == 0) {
            return;
        }
        newTag = newTag.trim().toLowerCase();
        if (_.indexOf($scope.talk.talkTags,newTag) === -1) {
            $scope.talk.talkTags.push(newTag);
        }
        $("#tagInput").val("").focus();
    }

    $scope.removeATag = function(atag) {
        $scope.talk.talkTags = _.without($scope.talk.talkTags,atag);
    }


    if (talkid === 0) {
        $scope.talk = {
            presentationType : "presentation",
            length: 60,
            title: "",
            abstract: "",
            language: "no",
            level: "beginner",
            outline: "",
            highlight: "",
            equipment: "",
            talkTags: [],
            expectedAudience: "",
            captchaFact: captchaFact,
            captchaAnswer: "",
            password: "",
            speakers: [{
                speakerName: "",
                email: "",
                bio: "",
                picture: null,
                zipCode: "",
                givenId: null,
                dummyId: getDummySpeakerId(),
                picurl: null,
                hasPicture: false
            }]
        };

        $scope.lengthDescription = typeDescription[$scope.talk.presentationType].text;
        $scope.talkLengths = typeDescription[$scope.talk.presentationType].lengths;

        $scope.canHaveMoreSpeakers = true;



        $scope.talk.speakers[0].ifurl="uploadPicture?speakerid=&dummyKey=" +
            $scope.talk.speakers[0].dummyId;

        $scope.submitClosed = false;

        $http({method: 'GET', url: "needPassword"}).
        success(function(data, status, headers, config) {
            $scope.needPassword = data.needPassword;
            $scope.submitClosed = data.isClosed;

        }).
        error(function(data, status, headers, config) {
            console.log("Error fetching password neeeded");
        });

    }

    $scope.tagList = [];


    $http({method: 'GET', url: "tagcoll.json"}).
            success(function(data, status, headers, config) {
                //var tagx = ['java','clojure','anders'];
                var tagx = data;
                $("#tagInput").autocomplete({source: tagx});
                if (talkid != 0) {
                    var jsonurl = "talkJson/" + talkid;


                    $http({method: 'GET', url: jsonurl}).
                        success(function(data, status, headers, config) {
                            $scope.showCapthca = false;
                            $scope.talk = data;
                            $scope.lengthDescription = typeDescription[$scope.talk.presentationType].text;
                            $scope.talkLengths = typeDescription[$scope.talk.presentationType].lengths;

                            $scope.talk.captchaFact = captchaFact;
                            $scope.talk.captchaAnswer = "";



                            $scope.talk.speakers.forEach(function (speaker) {
                                speaker.hasPicture = false;
                                $scope.computePictureUrl(speaker);
                                speaker.ifurl="uploadPicture?speakerid=" + speaker.givenId + "&dummyKey=";
                            });

                            $scope.canHaveMoreSpeakers = ($scope.talk.speakers.length < 2);

                        }).
                        error(function(data, status, headers, config) {
                            console.log("Could not load talk");
                            $scope.showMain = false;
                            $scope.failureError = "Could not find a talk with this id";
                            $scope.showFailure = true;
                        });
                    
                }



            }).
            error(function(data, status, headers, config) {
                console.log("some error occured");
            });



    $scope.addASpeaker = function(e) {
        e.preventDefault();
        var newsp = {
            speakerName: "",
            email: "",
            bio: "",
            picture: null,
            zipCode: "",
            givenId: null,
            dummyId: getDummySpeakerId()
        };
        newsp.ifurl="uploadPicture?speakerid=&dummyKey=" + newsp.dummyId;
        $scope.talk.speakers.push(newsp);

        $scope.canHaveMoreSpeakers = ($scope.talk.speakers.length < 2);
        $scope.canDeleteSpeaker = (talkid === 0) && ($scope.talk.speakers.length > 1);
    }

    
    $scope.talkSubmit = function(value) {
        $scope.showCapthcaError = false;
        $scope.showGeneralError = false;
        $scope.showUserErrorMessage = false;
        var submitBtn = $("#submitButton");     
        submitBtn.button('loading');

        var myForm = $('#submitForm');
        var valid = myForm[0].checkValidity();
        if (!valid) {
            submitBtn.button('reset');
            return true;
        }

        var displayUserError = function(message) {
            submitBtn.button('reset');
            $scope.showUserErrorMessage =true;
            $scope.userErrorMessage = message;
        };

        if ($scope.talk.talkTags.length > 3) {
            displayUserError("Your talk can maximum have three tags. Please delete one or more tags");
            return false;
        }



        $http({method: 'POST', url: "addTalk", data: $scope.talk}).
                        success(function(data, status, headers, config) {
                            submitBtn.button('reset');

                            if (data.captchaError) {
                                $scope.showCapthcaError = true;                             
                            } else if (data.errormessage) {
                                $scope.showMain = false;
                                $scope.showFailure = true;
                                $scope.failureError = data.errormessage;
                            } else {
                                $scope.showMain = false;
                                $scope.showResult = true;
                                $scope.talkAddress = data.addr;
                                if (data.retError) {
                                  $scope.showResultSuccess = false;
                                  $scope.showResutFailure = true;
                                } else {
                                    $scope.showResultSuccess = true;
                                    $scope.showResutFailure = false;
                                }
                            }
                            
                        }).
                        error(function(data, status, headers, config) {
                            submitBtn.button('reset');
                            $scope.showGeneralError = true;
                            console.log("some error occured");
                        });
        return false;
    }

    $scope.newPicture = function(dummyKey,speakerKey) {
        var speaker = _.find($scope.talk.speakers,function(speaker) {
            return speaker.dummyId == dummyKey;
        });
        speaker.hasPicture = true;
        $scope.computePictureUrl(speaker);
        $scope.$digest();

    };

    $scope.computePictureUrl = function(speaker) {
        if (speaker.hasPicture) {
            speaker.picurl = "tempPhoto?dummyId=" + speaker.dummyId + "&time=" + new Date().getTime();
            return;
        };
        if (speaker.picture && speaker.picture != null) {
            speaker.picurl = "speakerPhoto?photoid=" + speaker.picture + "&time=" + new Date().getTime();
            return;
        };
        if (speaker.email) {
            speaker.picurl = get_gravatar(speaker.email);
        };

    };

    $scope.canDeleteSpeaker = false;
    $scope.deleteSpeaker = function(speaker) {
        var ind = _.indexOf($scope.talk.speakers,speaker);
        if (ind !== -1) {
            $scope.talk.speakers.splice(ind, 1);
        }

        $scope.canHaveMoreSpeakers = ($scope.talk.speakers.length < 2);
    };





}]);