"use strict";

var SubmitFormModel = Backbone.Model.extend({

});

var SubmitFormView = Backbone.View.extend({
	events: {
            "click #submitButton": "submitClicked"
    },

	initialize: function (attrs) {
		this.template = _.template(attrs.template);
	},

	render: function() {
		this.$el.html(this.template(this.model.toJSON()));
	},

	submitClicked: function() {
		var myForm = this.$('#submitForm');
		var valid = myForm[0].checkValidity();
		if (!valid) {
			return true;
		}

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

		this.model.url="/addTalk";

		this.model.save();


		return false;
	}

});

$(function() {
	var fv=new SubmitFormView({
		model: new SubmitFormModel,
		el: $("#main"),
		template: $("#submitFormTemplate").html()
	})
	fv.render();
});