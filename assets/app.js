console.log ("Hello world");

function getJson(url) {
    return new Promise((resolve, reject) => {
        $.ajax({url: url, dataType: 'json', success: resolve, error: reject});
    })
}

function getHtml(url) {
    return new Promise((resolve, reject) => {
        $.ajax({url: url, dataType: 'html', success: resolve, error: reject});
    })
}

$(function(){

    Promise.all([getHtml('./template_room.html'), getJson('./musicrooms')])
        .then(([templateString, roomData]) => {
            console.log("musicrooms", roomData);

            let rooms = roomData.music_rooms;
            let container = document.getElementsByClassName('container')[0];

            let roomTemplateString = templateString;

            let roomTemplate = _.template(roomTemplateString);

            for(let i = 0; i < rooms.length; i++){
                let room = rooms[i];
                console.log('room:', room);

                let elemString = roomTemplate(room);

                console.log(elemString);
                $(container).append(elemString);
            }
        })

});