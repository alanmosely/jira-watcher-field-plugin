AJS.$(document).ready(function(){
	AJS.$('#toggle-watch-issue').click(function(event){
		console.log(event);
	});
});
//AJS.$('#toggle-watch-issue').click(function(event){console.log(event);});
/*
AJS.$.ajax({ 
		url:contextPath + "/rest/api/1.0/issues/10000/watchers",
		type: "POST",
		dataType: "json",
		success: function (response) {
			console.log('success');
			console.log(response);
		},
		error: function(XMLHttpRequest, textStatus, errorThrown){
			console.log('error');
			console.log(XMLHttpRequest);
			console.log(textStatus);
			console.log(errorThrown);
		}
});

AJS.$('#toggle_watching_label').ajaxComplete(function(event, XMLHttpRequest, ajaxOptions) {
	var regex = new RegExp("(.*)/rest/api/(.+)/issues/(.+)/watchers(.*)", "ig");
	if(regex.test(ajaxOptions.url)){
		AJS.$.ajax({ 
			url: contextPath + "/rest/watcherfield/latest/watchers?issueid=TST-1",
			type: "GET",
			dataType: "json",
			success: function (response) {
				var html = '';
				if(response.watchers !== undefined){
					html += '<span class="tinylink">';
					AJS.$(response.watchers).each(function(index, value){
						html += '<a href="/jira/secure/ViewProfile.jspa?name='+value+'" id="multiuser_cf_'+value+'">'+value+'</a>';
					});
					html += '</span>';
				}
				AJS.$('#customfield_10000-field').html(html);
			},
			error: function(XMLHttpRequest, textStatus, errorThrown){
				console.log("Error in ajax call in watcherfieldRest.");
				console.log(XMLHttpRequest);
			}
		});
	}
});
*/