console.log("Hello world");

_.merge(WebSocket.prototype, Object.create(EventEmitter.prototype))

class AppView extends React.Component {

    constructor(props) {
        super(props || {});
        EventEmitter.call(this);

        this.deferreds = {};
        this.retryCount = {server: 0};
        this.models = {};
        this.tokens = null;
        this.mainContentElem = React.createRef();

        this.state = {
            user: null,
            musicrooms: [],
            newStack: [],
            newMusicroom: null,
            searchQueryMusicroom: null,
            searchQuerySong: null,
            selectedRoom: null,
        }
    }

    searchTracks(term) {
        return this.spotify(`search?q=${encodeURIComponent(term)}&type=track&limit=16`, {method: 'GET'})
    }

    getSpotifyOAuthToken() {
        return this.getJson("spotify_token").then((tokens) => {
            if (_.isPlainObject(tokens)) {
                this.tokens = toCamelCase(tokens);
                return this.tokens;
            }
        })
    }

    set newMusicroom(m) {
        let old = this.newMusicroom
        this.setState({newMusicroom: m})

        if (_.isPlainObject(old) && m == null) {

        }

        if (m === null) this._room = null
    }

    set selectedRoom(room) {
        if (room == null) return this.setState({selectedRoom: null})
        this.setState({selectedRoom: room})
    }

    set musicroomView(room) {
        if (room.state && this.state.selectedRoom && this.state.selectedRoom.id === room.state.id) this._room = room
    }

    get musicroomView() {
        return this._room
    }

    get newMusicroom() {
        return this.state.newMusicroom
    }

    spotify(endpoint, opt) {
        return this.getSpotifyOAuthToken()
            .then(tokens => {
                let method = (opt || {}).method || 'PUT'

                let fetchOpts = {
                    method,
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${tokens.accessToken}`
                    },
                }

                if(_.includes(['PUT', 'HEAD'], method.toUpperCase()) && _.isObject(opt)){

                    //uris: [spotify_uri]
                    fetchOpts.body = JSON.stringify(opt)
                }
                let bar = _.includes(endpoint, '?') ? '&' : '?';
                let url = `https://api.spotify.com/v1/${endpoint}${bar}device_id=${this.spotifyPlayer._options.id}`
                return fetch(url, fetchOpts).then(r => r.json());
            })
    }

    play(spotifyUri) {
        if(spotifyUri && !_.startsWith(spotifyUri, 'spotify:track:')){
            spotifyUri = `spotify:track:${spotifyUri}`
        }
        spotifyUri = spotifyUri || 'spotify:track:4eTgQdjd7bIet6d045GGUc'
        return this.spotify('me/player/play', {uris: [spotifyUri]})
    }

    pause(spotifyUri) {
        if(spotifyUri && !_.startsWith(spotifyUri, 'spotify:track:')){
            spotifyUri = `spotify:track:${spotifyUri}`
        }
        spotifyUri = spotifyUri || 'spotify:track:4eTgQdjd7bIet6d045GGUc'

        return this.spotify('me/player/pause', {uris: [spotifyUri]})
    }

    get spotifyPlayer() {
        if (!this._spotifyPlayer) {
            this._spotifyPlayer = new Spotify.Player({
                name: 'PlayThis',
                getOAuthToken: (cb) => {
                    this.getSpotifyOAuthToken()
                        .then((tokens) => {
                            console.log('[tokens]', tokens)
                            window.tokens = tokens
                            cb(tokens.accessToken)
                        });
                }
            });
        }

        return this._spotifyPlayer
    }

    initSpotify() {
        //if(window.onSpotifyWebPlaybackSDKReady) return

        if (!window.onSpotifyWebPlaybackSDKReadyCalled) {
            console.log('[spotify] initialising player deferred')
            return setTimeout(this.initSpotify.bind(this), 250);
        }

        console.log('[spotify] initialising player')

        //const token = 'BQC0qUoIG-IEhPIIRTXU96oxsKP1FdTEiMo3_mVtUO1SaX89U3uabi6ucOEQkzMzzgg-rn0v0izeZ6pm8VJ5Pw9g5gewtVh4MzbCxpCyhS13exzhUZ8fi5FWnCbCFDFXzGRGwpmFttY4yngbAm0CGc2_1Cjfg0Z_o7IH';
        //const token = 'BQDn-p_Xe11EdRf-lyWDebj0-WLf7yFg6IZ-S0AfzYK_mgKGYpjJaTwn6zs8KGTnSyylE5N1x8N4Fh7lrL4W9creZ_IVN3nF7HvUNRDTz9l5mtzcWF_Qb2sAXoKfXcXnQW9L566IfbPV6TTDHD3wEVuAVo3vsId37GBo8ErKXvk_IDO4SFJVaA';

        let spotifyPlayer = this.spotifyPlayer

        // Error handling
        spotifyPlayer.addListener('initialization_error', ({message}) => {
            console.error(message);
        });
        spotifyPlayer.addListener('authentication_error', ({message}) => {
            console.error(message);
        });
        spotifyPlayer.addListener('account_error', ({message}) => {
            console.error(message);
        });
        spotifyPlayer.addListener('playback_error', ({message}) => {
            console.error(message);
        });

        // Playback status updates
        spotifyPlayer.addListener('player_state_changed', state => {
            console.log('[player_state_changed]', state);
        });

        // Ready
        spotifyPlayer.addListener('ready', ({device_id}) => {
            console.log('Ready with Device ID', device_id);
        });

        // Not Ready
        spotifyPlayer.addListener('not_ready', ({device_id}) => {
            console.log('Device ID has gone offline', device_id);
        });

        // Connect to the player!
        spotifyPlayer.connect();
        return new Date();
    }

    get name() {
        return 'PlayThis'
    }

    getSchemas() {
        if (_.isPlainObject(this._modelSchemas)) Promise.resolve(this._modelSchemas);

        return this.getJson('schemas')
            .then((res) => {
                console.log('[getSchemas]', res);

                _.each(res, (schema, modelName) => {
                    this.models[modelName] = DbModel.setSchema(modelName, schema);
                })
            })
    }

    getJson(url) {
        return new Promise((resolve, reject) => {
            $.ajax({url: url, dataType: 'json', success: resolve, error: reject});
        })
    }

    getHtml(url) {
        return new Promise((resolve, reject) => {
            $.ajax({url: url, dataType: 'html', success: resolve, error: reject});
        })
    }

    musicroomsFiltered() {
        let rooms = this.state.musicrooms || [];
        let query = this.state.searchQueryMusicroom;

        if (this.state.searchQueryMusicroom) {
            rooms = _.filter(rooms, (r) => _.isString(r.name) && _.includes(r.name.toLowerCase(), query.toLowerCase()))
        }

        return rooms;
    }

    render() {
        let style = {width: '100%', height: '100%'};
        let mainContentStyle = {height: 'calc(100% - 120px)', outline: 'none', overflow: 'auto'};

        let mainContent;
        let mKey = 'maincontent_default'

        if (_.isPlainObject(this.state.newMusicroom)) {
            mainContent = e(Musicroom, Object.assign({
                viewFormat: VIEW_FORMAT.FULL,
                editMode: true
            }, this.state.newMusicroom));
            mKey = 'maincontent_edit_music'
        } else if (this.state.selectedRoom) {
            mainContent = e(Musicroom, Object.assign({
                viewFormat: VIEW_FORMAT.FULL,
                editMode: false
            }, this.state.selectedRoom));
        } else {
            mainContent = e(ModelCollection, {
                model: Musicroom,
                modelsProps: _.map(this.musicroomsFiltered(), (m) => Object.assign({viewFormat: VIEW_FORMAT.GRID_ITEM}, m))
            });
        }

        let placeholderSuffix = _.isObject(this.state.selectedRoom) ? 'Spotify' : 'music rooms';

        return e.div({style}, [
            e(Navbar, {key: 'navbar'}),
            e(Toolbar, {key: `toolbar ${placeholderSuffix}`, placeholder: `Search ${placeholderSuffix}`}),
            e.div({style: mainContentStyle, className: 'home', ref: this.mainContentElem, tabIndex: 0, key: mKey}, mainContent)
        ])
    }

    onServerConnected() {
        console.log('[server connected]')

        app.server.on('db_update', (update) => {
            console.log('[db_update]', update)
        })
    }

    onServerDisconnected() {
        console.log('[server disconnected]')

        setTimeout(() => this.connectToServer(), 1000 * app.retryCount.server)
    }

    get navbar() {
        return window.appNavbar
    }

    get toolbar() {
        return window.appToolbar
    }

    componentDidMount() {

        let $elem = $(this.mainContentElem.current)
        $elem.on('focusin', () => {
            console.log('[focus#app]')
            $(app.toolbar.dropdown.current).hide()
        })

        window.app = this;
        this.initSpotify();

        this.connectToServer().then(() => {
            console.log('[connectToServer] callback')
            //if(this.server.isOpen()) this.onServerConnected()

            this.server.on('server_connected', this.onServerConnected.bind(this))
            this.server.on('server_disconnected', this.onServerDisconnected.bind(this))
        })

        this.getSchemas()

        this.getJson('./db/musicrooms')
            .catch(function () {
                console.error("hey", arguments)
            })
            .then((roomData) => {
                let rooms = toCamelCase(roomData.data);
                this.setState({musicrooms: rooms});
            })
    }

    connectToServer() {
        let url = `${location.protocol === 'https:' ? 'wss://' : 'ws://'}${location.host}/ws`
        return this.connect(url, {
            connected: 'server_connected',
            disconnected: 'server_disconnected',
            retryCount: 'server'
        }).then((server) => {
            this.server = server
        })
    }

    connect(url, options = {}) {
        let ws = new WebSocket(url)
        ws.sessionId = null

        EventEmitter.call(ws)

        this.retryCount[options.retryCount]++
        let name = options.retryCount

        this[name] = ws

        let subscriptions = ws.subscriptons = {db_update: 0}

        ws.isOpen = () => {
            return ws.readyState === WebSocket.OPEN
        }

        ws.on = function (eventName, callback) {
            if (_.includes(['server_connected', 'server_disconnected'], eventName)) return EventEmitter.prototype.on.call(ws, eventName, callback)

            let topic = `/${eventName}`
            console.log(`${name}: event=${eventName}, topic=${topic}`)

            let subscribe = `/subscribe?subject=${eventName}`
            ws.json(subscribe)
                .then((resp) => {
                    subscriptions[resp.data.subscribed] = 0
                    console.log('subscriptions:', subscriptions)

                    EventEmitter.prototype.on.call(ws, eventName, callback)
                })
        }

        ws.json = (topic, data) => {
            let deferred = new Deferred()
            this.deferreds[deferred.id] = deferred

            if (!_.isPlainObject(data)) data = {}

            data.ACCESS_TOKEN = this.accessToken
            let req = {topic: `${topic}${_.includes(topic, '?') ? '&' : '?'}uid=${deferred.id}`, data: data}
            ws.send(JSON.stringify(req))
            return deferred
        }

        ws.onmessage = (message) => {
            let msg = JSON.parse(message.data)
            let parts = _.split(msg.topic, '?', 2)

            let parsed = _.chain(_.last(parts))
                .replace('?', '') // a=b454&c=dhjjh&f=g6hksdfjlksd
                .split('&') // ["a=b454","c=dhjjh","f=g6hksdfjlksd"]
                .map(_.partial(_.split, _, '=', 2)) // [["a","b454"],["c","dhjjh"],["f","g6hksdfjlksd"]]
                .fromPairs() // {"a":"b454","c":"dhjjh","f":"g6hksdfjlksd"}
                .value()

            let deferredId = parsed.uid
            let deferred = this.deferreds[deferredId]
            let topic = _.first(parts)

            if (_.isObject(deferred)) {
                delete this.deferreds[deferredId]

                if (msg.error) deferred.reject(new Error(`[${name}] ${topic}: ${msg.error}`))
                else deferred.resolve(msg)
            } else if (_.includes(['/subscribe', '\\/subscribe'], topic) && parsed.subject in subscriptions) {
                subscriptions[parsed.subject]++
                ws.emit(parsed.subject, msg)
            } else if (_.includes(['/sessionid', '\\/sessionid'], topic)) {
                ws.sessionId = parsed.id
                console.log('[websocket] got sessing id:', ws.sessionId)
            } else {
                console.error('no pending promise', message, topic, parsed)
            }
        }

        return new Promise((resolve, reject) => {
            ws.onopen = () => {
                this.server = ws;
                this.retryCount[options.retryCount] = 0
                resolve(ws)

                setTimeout(() => ws.emit(options.connected || 'server_connected'))
            }

            ws.onclose = () => {
                reject()
                ws.emit(options.disconnected || 'server_disconnected')
            }

            ws.onerror = (err) => {
                console.error(err)
                reject(err)
            }
        })
    }

}

_.merge(AppView.prototype, Object.create(EventEmitter.prototype))