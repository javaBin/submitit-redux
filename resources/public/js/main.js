"use strict";

$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results ? results[1] || 0 : 0;
}


var SpeakerModel = Backbone.Model.extend({

});


var SpeakerCollection = Backbone.Collection.extend({
	model: SpeakerModel
});

var SpeakerView = Backbone.View.extend({
	events: {
            "change .speakerInput" : "speakerInputChanged",
            "change #speakerPicture" : "speakerPictureAdded"
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	},

	speakerInputChanged: function() {
		var self = this;
		self.model.set({
			speakerName: self.$("#speakerNameInput").val(),
			email: self.$("#emailInput").val(),
			bio: self.$("#speakerBioInput").val()
		});
	},

	speakerPictureAdded: function(eve) {

		var givenFile = eve.target.files[0];

		var reader = new FileReader();

		var self = this;

		reader.onload = (function(theFile) {
			return function(e) {
				console.log(e.target.result);
				self.model.set({
					picture: e.target.result
				});
				self.render();
			}
		})(givenFile);

		reader.readAsDataURL(givenFile);

	}

})

var SubmitFormModel = Backbone.Model.extend({
	

});

var SubmitFormView = Backbone.View.extend({
	events: {
            "click #submitButton": "submitClicked",
            "change .talkInput" : "inputChanged",
            "click .talkInput" : "inputChanged"            
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
		this.speakerTemplateText = attrs.speakerTemplate;
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
		var speakerDom = this.$("#speakerList");
		var self = this;
		this.model.get("speakers").each(function(speakerModel) {
			var speakerView = new SpeakerView({
				model: speakerModel,
				template: self.speakerTemplateText
			});
			speakerView.render();
			speakerDom.append(speakerView.el);

		});
	},

	submitClicked: function() {
		var myForm = this.$('#submitForm');
		var valid = myForm[0].checkValidity();
		if (!valid) {
			return true;
		}

		this.model.url="/addTalk";

		console.log(this.model);

		this.model.save();


		return false;
	},

	inputChanged: function() {
		var self = this;

		this.model.set({
			presentationType : self.$("#presentationTypeGrp button.active").val(),
			title: self.$("#titleInput").val(),
			abstract: self.$("#abstractInput").val(),
			language: self.$("#languageGrp button.active").val(),
			level: self.$("#levelGrp button.active").val(),
			outline: self.$("#outlineInput").val(),
			highlight: self.$("#highlightInput").val(),
			equipment: self.$("#equipmentInput").val(),
			expectedAudience: self.$("#expectedAudienceInput").val()
		});
	},



	
});

$(function() {	
	var givenId = $.urlParam("talkid");
	var submitFormModel;
	if (givenId === 0) {
		submitFormModel = new SubmitFormModel({
			presentationType : "",
			title: "",
			abstract: "",
			language: "no",
			level: "",
			outline: "",
			highlight: "",
			equipment: "",
			expectedAudience: "",
			speakers: new SpeakerCollection({
				speakerName: "",
				email: "",
				bio: "",
				picture: null
			})
		});
	} else {
		submitFormModel = new SubmitFormModel;
		submitFormModel.fetch({
			async : false,
			url : "talkJson?talkid=" + givenId 
		});
		var speakArr = submitFormModel.get("speakers");
		var speakColl = new SpeakerCollection(speakArr);
		console.log(speakColl);
		submitFormModel.set({
			speakers: speakColl
		}, {silent: true});
	}
	

	var fv=new SubmitFormView({
		model: submitFormModel,
		el: $("#main"),
		template: $("#submitFormTemplate").html(),
		speakerTemplate: $("#speakerTemplate").html()
	})
	fv.render();
});