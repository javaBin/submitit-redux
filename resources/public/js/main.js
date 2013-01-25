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
			bio: self.$("#speakerBioInput").val(),
			zipCode: self.$("#speakerZipCodeInput").val()
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

});

var ResultModel = Backbone.Model.extend({
	
});

var ResultView = Backbone.View.extend({
	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	}
});

var TagModel = Backbone.Model.extend({

});

var TagCollection = Backbone.Collection.extend({
	model: TagModel,

	setupChecked : function (talkTags) {
		this.each(function(tagModel) {
			var toCheck = (talkTags.indexOf(tagModel.get("value")) !== -1);
			tagModel.set({
				checked: toCheck
			},{silent:true});
		});
	}
});

var TagView  = Backbone.View.extend({
	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.append(this.template(this.model.toJSON()));
	}
});

var SubmitFormModel = Backbone.Model.extend({
	

});




var SubmitFormView = Backbone.View.extend({
	events: {
            "click #submitButton": "submitClicked",
            "change .talkInput" : "inputChanged",
            "click .talkInput" : "inputChanged",
            "click .tagCheckbox" : "tagCheckboxChanged"            
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
		this.speakerTemplateText = attrs.speakerTemplate;
		this.resultTemplateText = attrs.resultTemplate;
		this.tagTemplate = attrs.tagTemplate;
		this.tagCollection = attrs.tagCollection;
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

		var tagDom = this.$("#tagBoxes");

		this.tagCollection.each(function(tagModel) {
			var tagView = new TagView({
				model: tagModel,
				template: self.tagTemplate
			});
			tagView.$el = tagDom;
			tagView.render();
		});
	},

	submitClicked: function() {
		var myForm = this.$('#submitForm');
		var valid = myForm[0].checkValidity();
		if (!valid) {
			return true;
		}

		this.model.url="/addTalk";

		var self = this;

		var talkDetail = window.location.origin + "/talkDetail?talkid=";

		this.model.save({}, {success: function(model, response){
			var resultModel = new ResultModel({
				talkAddress: talkDetail + response.resultid
			});
			var resultView = new ResultView({
				model: resultModel,
				template: self.resultTemplateText
			});
			resultView.render();
			self.$el.html(resultView.el);
		}});



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

	tagCheckboxChanged: function(e) {
		var talkList = this.model.get("talkTags");

		if (e.currentTarget.checked) {
			talkList.push(e.currentTarget.value);
		} else {
			talkList.splice(talkList.indexOf(e.currentTarget.value), 1);
		}
		this.model.set({
			talkTags: talkList
		});
	}



	
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
			level: "beginner",
			outline: "",
			highlight: "",
			equipment: "",
			talkTags: [],
			expectedAudience: "",
			speakers: new SpeakerCollection({
				speakerName: "",
				email: "",
				bio: "",
				picture: null,
				zipCode: ""
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

	var tagCollection = new TagCollection([
		 {value : "Alternative languages"},
		 {value : "Architecture in practise"},
		 {value : "Big Data and NoSQL"},
		 {value : "Continous delivery"},
		 {value : "Core Java"},
		 {value : "Distributed systems and cloud"},
		 {value : "Enterprise"},
		 {value : "Experience report"},
		 {value : "Frontend"},
		 {value : "Functional Programming"},
		 {value : "Innovation and Startups"},
		 {value : "Mobile"},
		 {value : "Research and Trends"},
		 {value : "Security"},
		 {value : "Craftsmanship and Tools"}
		]);
	
	tagCollection.setupChecked(submitFormModel.get("talkTags"));

	console.log(tagCollection);

	var fv=new SubmitFormView({
		model: submitFormModel,
		tagCollection: tagCollection,
		el: $("#main"),
		template: $("#submitFormTemplate").html(),
		speakerTemplate: $("#speakerTemplate").html(),
		resultTemplate: $("#resultTemplate").html(),
		tagTemplate: $("#tagTemplate").html()
	})
	fv.render();
});