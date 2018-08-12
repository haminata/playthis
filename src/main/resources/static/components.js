/**
 * Created by haminata on 04/08/2018.
 */

const e = React.createElement;

Object.assign(e, ReactDOMFactories);

const VIEW_FORMAT = {
    LIST_ITEM: 'list_item',
    GRID_ITEM: 'grid_item',
    FULL: 'full'
};

class DbModel extends React.Component {

    constructor(props) {
        super(props || {});
        this.state = Object.assign({viewFormat: VIEW_FORMAT.FULL}, this.props);
        //this.constructor.cache.push(this);
    }

    set viewFormat(format) {
        this.setState({viewFormat: format});
    }

    get viewFormat() {
        return this.state.viewFormat || VIEW_FORMAT.DEFAULT;
    }

    get style() {
        return {}
    }

    render() {
        return e('h1', {ref: window.textInput}, 'Hello World')
    }

    static get cache() {
        if (this.name === DbModel.name) {
            return this.CACHE;
        } else {
            let clsName = this.name;
            let models = DbModel.CACHE[clsName] = DbModel.CACHE[clsName] || [];
            return models;
        }
    }

    static get schema() {
        return DbModel.SCHEMAS[this.name]
    }

    get schema() {
        return this.constructor.schema;
    }

    static get namePlural() {
        return this.schema.plural;
    }

    get namePlural() {
        return this.constructor.namePlural;
    }

    get isNew() {
        return !_.isInteger(this.state.id);
    }

    static all() {
        return app.getJson(`${this.namePlural}`)
            .then((resp) => resp.data)
    }

    all() {
        return this.constructor.all();
    }

    static getModelClass(modelName) {
        let cls = _.attempt(eval, modelName);
        if (cls && !_.isError(cls)) return cls;
        else return null;
    }

    static setSchema(modelName, schema) {
        this.SCHEMAS[modelName] = schema;


        let cls = this.getModelClass(modelName);

        if (!cls) return;

        _.each(schema.properties, (s, propName) => {
            let desc = Object.getOwnPropertyDescriptor(cls.prototype, propName);

            if (!desc || !desc.get) {
                Object.defineProperty(cls.prototype, propName, {
                    get: function () {
                        return this.state[propName] || this.props[propName];
                    },
                    set: function (value) {
                        this.setState({propName: value});
                    }
                })
            }
        })
    }
}

DbModel.SCHEMAS = {};
DbModel.CACHE = {};

class Musicroom extends DbModel {

    constructor(props) {
        super(props || {})
        this.form = React.createRef();
        window.newroom = this;
    }

    get createdBy() {
        return _.isObject(this.state.createdBy) ? this.state.createdBy : {name: 'Anonymous'}
    }

    get data() {
        return _.transform(newroom.form.current.querySelectorAll('[name]'), (r, v) => {
            let name = _.startsWith(v.name, 'room_') ? v.name.slice(5) : v.name;
            if (v.type === "radio" && v.name in r) {
                return
            } else if (v.type === "radio") {
                let members = newroom.form.current.querySelectorAll(`[name="${v.name}"]`);
                let checked = _.find(members, {checked: true})
                if (checked) {
                    r[name] = v.value
                }
            } else {
                r[name] = v.value
            }

            if(name === 'created_by') r[name] = _.toInteger(v.value) // hack
        }, {id: this.state.id})
    }

    onPublish() {
        let data = this.data
        app.server.json('/create_room', data, {busy: true})
            .then((response) => {
                console.log('[onPublish] ', response)
            })
    }

    componentDidMount() {
        //app.setState({newMusicroom: this})
        //document.getElementById("form_create_musicroom").classList.remove('hidden')
        if (this.isNew) app.getJson("/db/users").then((res) => {
            console.log('[load users]', res)
            this.setState({users: res.data || []})
        })
    }

    onCancel() {
        app.setState({newMusicroom: null})
    }

    onUserAdminChange(event) {
        this.setState({value: event.target.value});
    }

    onDescriptionChange(event){
        this.setState({description: event.target.value})
    }

    renderEditor() {
        let users = _.map(this.state.users || [], (user) => {
            return e.option({value: user.id}, user.name);
        })

        return e.div({className: 'container'}, [
            e.h1({className: 'display-4'}, this.isNew ? 'Create Music Room' : (this.state.name || '[No Title]')),
            e.p({className: 'lead'}, 'Projects are where your repositories live. They are containers you can group similar repositories in for better code organisation.'),
            e.form({className: 'hidden', ref: this.form, id: 'form_create_musicroom'}, [
                e.div({className: 'form-group required'}, [
                    e.label({htmlFor: 'exampleInputEmail1', className: 'col-form-label'}, 'Project Name'),
                    e.input({
                        type: 'text',
                        className: 'form-control',
                        name: 'room_name',
                        value: this.state.name,
                        id: 'exampleInputEmail1',
                        placeholder: 'Haminata\'s House Party',
                        required: true
                    })
                ]),
                e.div({className: 'form-group'}, [
                    e.label({htmlFor: 'exampleFormControlTextarea1', className: 'col-form-label'}, 'Description'),
                    e.textarea({
                        className: 'form-control',
                        name: "room_description",
                        value: this.state.description || '',
                        rows: "3",
                        id: 'exampleFormControlTextarea1',
                        onChange: this.onDescriptionChange.bind(this)
                    })
                ]),

                e.div({className: 'row'}, [
                    e.div({className: 'col-md-3'}, [
                        e.fieldset({className: ''}, [
                            e.legend({className: ''}, 'Entitlement Level'),
                            e.div({className: 'form-check'}, [
                                e.input({
                                    className: "form-check-input",
                                    type: "radio",
                                    name: "room_entitlement_level",
                                    value: "entitlement_level_addsongs",
                                    defaultChecked: "true",
                                    id: "defaultCheck1a"
                                }),
                                e.label({
                                    htmlFor: 'defaultCheck1a',
                                    className: 'form-check-label'
                                }, 'Vote and suggest songs'),
                            ]),
                            e.div({className: 'form-check'}, [
                                e.input({
                                    className: "form-check-input",
                                    type: "radio",
                                    name: "room_entitlement_level",
                                    value: "entitlement_level_voteonly",
                                    id: "defaultCheck2a"
                                }),
                                e.label({htmlFor: 'defaultCheck2a', className: 'form-check-label'}, 'Vote only'),
                            ]),
                        ]),
                    ]),
                    e.div({className: 'col-md-3'}, [
                        e.fieldset({style: {className: ''}, className: ''}, [
                            e.legend({className: ''}, 'Guest Access'),
                            e.div({className: 'form-check'}, [
                                e.input({
                                    className: "form-check-input",
                                    type: "radio",
                                    name: "room_access",
                                    value: "room_access_inviteonly",
                                    id: "defaultCheck1"
                                }),
                                e.label({htmlFor: 'defaultCheck1', className: 'form-check-label'}, 'Invite Only'),
                            ]),
                            e.div({className: 'form-check'}, [
                                e.input({
                                    className: "form-check-input",
                                    type: "radio",
                                    name: "room_access",
                                    defaultChecked: "true",
                                    value: "room_access_public",
                                    id: "defaultCheck2"
                                }),
                                e.label({htmlFor: 'defaultCheck2', className: 'form-check-label'}, 'Public'),
                            ]),
                        ]),
                    ]),
                    e.div({className: 'col-md-3'}, [
                        e.fieldset({style: {className: ''}, className: ''}, [
                            e.legend({className: ''}, 'Room Admin'),
                            e.div({className: 'form-group'}, [
                                //<label for="exampleFormControlSelect1">Example select</label>
                                e.label({className: "", htmlFor: "defaultCheck1d"}, "Example select"),
                                e.select({
                                    className: "form-control",
                                    name: "room_created_by",
                                    value: (_.first(users) || {}).name,
                                    id: "defaultCheck1d",
                                    onChange: this.onUserAdminChange.bind(this)
                                }, users),
                            ]),
                        ]),
                    ]),
                ]),
                e.br(),
                e.div({className: 'form-group row'}, [
                    e.div({className: 'col-sm-2'}, [
                        e.button({
                            type: 'button',
                            className: 'btn btn-primary',
                            onClick: this.onPublish.bind(this)
                        }, 'Publish')
                    ]),
                    e.div({className: 'col-sm-2'}, [
                        e.button({
                            type: 'button',
                            className: 'btn btn-link text-danger',
                            onClick: this.onCancel.bind(this)
                        }, 'Cancel')
                    ])
                ])
            ])
        ])
    }

    render() {

        if (this.state.editMode) return this.renderEditor()

        const style = {
            backgroundColor: this.name,
            maxHeight: '20vh',
            marginTop: '16px'
        };
        let key = `${this.constructor.name}_${this.state.id}`;
        let onJoinClick = () => {
            app.setState({selectedRoom: Object.assign({editMode: true}, this.state)});
        };

        return e.div({style, key, className: 'card'}, [
            //e.img({className: 'card-img-top', alt: 'Card image cap', src: ".../100px180/"}),
            //e.div({className: 'card-header'}, this.props.name || 'No header'),
            e.div({className: 'card-body'}, [
                e.h5({className: 'card-title', key}, this.state.name || 'No title'),
                e.h6({className: 'card-subtitle mb-2 text-muted'}, `By ${this.createdBy.name}`),
                e.p({className: 'card-text'}, this.state.description || 'No description'),
                e.a({className: 'card-link', href: '#', onClick: onJoinClick}, 'Join'),
            ])
        ])
    }
}

class ModelCollection extends React.Component {

    render() {
        let elems = _.map(this.props.modelsProps || [], (prop) => {
            let key = `${this.props.model.name}_${prop.id}`;
            let el = e(this.props.model, _.merge({key, viewFormat: VIEW_FORMAT.GRID_ITEM}, prop));

            let style = {};
            return e('div', {key, className: 'col-md-4 col-lg-4 col-sm-6', style}, [el]);
        });

        return e.div({className: 'container'}, e.div({className: 'row'}, elems));
    }

}

class Navbar extends React.Component {

    onCreateMusicroom() {
        app.setState({newMusicroom: app.state.newMusicroom || {}})
    }

    onSignIn() {
        console.log('[Navbar] sign in')
    }

    render() {
        let children = [
            e.a({className: 'navbar-brand', href: '/'}, app.name),
            e.button({
                className: 'navbar-toggler',
                type: "button",
                'data-toggle': "collapse",
                'data-target': "#navbarText"
            }, [
                e.span({className: 'navbar-toggler-icon'})
            ]),
            //<button class="btn btn-outline-success my-2 my-sm-0 ml-2" data-emit-click="create_room">Create Room</button>
            e.ul({className: 'nav nav-tab navbar-nav mr-auto'}),
            e.button({
                className: 'btn btn-outline-success my-2 my-sm-0 ml-2',
                onClick: this.onCreateMusicroom.bind(this)
            }, 'Create Room'),
            e.button({
                className: 'btn btn-link my-2 my-sm-0 ml-2',
                onClick: this.onSignIn.bind(this)
            }, 'Sign In')
        ];
        return e.nav({className: 'nav navbar navbar-dark navbar-expand-sm bg-dark'}, e.div({className: 'container'}, children))
    }
}

class Toolbar extends React.Component {

    constructor(props) {
        super(props || {});
        this.state = Object.assign({placeholder: 'Search music rooms'}, this.props);
        this.input = React.createRef();
        console.log('[Toolbar]', this.props, props)
    }

    get searchPlaceholder() {
        return this.state.placeholder || ''
    }

    onChange() {
        console.log('[input]', this.input.current.value);
        app.setState({searchQueryMusicroom: this.input.current.value});
    }

    render() {
        return e.div({className: "alert alert-warning rounded-0", role: "alert"}, [
            e.div({className: 'container'}, [
                e.div({className: 'input-group input-group-lg w-100'}, [
                    e.input({
                        type: 'text',
                        className: 'form-control py-2 border-right-0 border bg-light',
                        placeholder: this.searchPlaceholder,
                        ref: this.input,
                        onChange: this.onChange.bind(this)
                    }),
                    e.span({className: 'input-group-append border-light'}, [
                        e.div({className: 'input-group-text border bg-light'}, [
                            e.i({className: 'fa fa-search'}),
                        ])
                    ])
                ])
            ])
        ]);
    }

}

class Deferred {

    constructor() {
        this.id = `${this.constructor._ID++}`

        this.promise = new Promise((res, rej) => {
            this.resolve = res
            this.reject = rej
        })
    }

    cancel() {
        this.reject('Cancelled!')
    }

    then(...args) {
        this._promise = (this._promise || this.promise).then(...args)
        return this
    }

    catch(...args) {
        this._promise = (this._promise || this.promise).catch(...args)
        return this
    }
}

Deferred._ID = 1

_.merge(WebSocket.prototype, Object.create(EventEmitter.prototype))

class AppView extends React.Component {

    constructor(props) {
        super(props || {});
        this.deferreds = {};
        this.retryCount = {server: 0}
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
        let mainContentStyle = {height: 'calc(100% - 50px)'};

        let mainContent;

        if (_.isPlainObject(this.state.newMusicroom)) {
            mainContent = e(Musicroom, Object.assign(this.state.newMusicroom, {
                viewFormat: VIEW_FORMAT.FULL,
                editMode: true
            }));
        } else if (this.state.selectedRoom) {
            mainContent = e(Musicroom, Object.assign({}, this.state.selectedRoom));
        } else {
            mainContent = e(ModelCollection, {
                model: Musicroom,
                modelsProps: this.musicroomsFiltered()
            });
        }

        return e.div({style}, [
            e(Navbar),
            e(Toolbar, {placeholder: `Search ${_.isObject(this.state.selectedRoom) ? 'Spotify' : 'music rooms'}`}),
            e.div({style: mainContentStyle}, mainContent)
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

    componentDidMount() {
        window.app = this;

        this.connectToServer().then(() => {
            this.server.on('server_connected', this.onServerConnected.bind(this))
            this.server.on('server_disconnected', this.onServerConnected.bind(this))
        })

        this.getSchemas()

        this.getJson('./db/musicrooms')
            .catch(function () {
                console.log("hey", arguments)
            })
            .then((roomData) => {
                let rooms = toCamelCase(roomData.data);
                console.log("musicrooms", rooms);
                this.setState({musicrooms: rooms});

                let container = document.getElementsByClassName('container')[0];
                let args = {
                    modelsProps: rooms,
                    model: Musicroom
                };


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

        let subscriptions = ws.subscriptons = {}

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
            } else if (topic === '/subscribe' && parsed.subject in subscriptions) {
                subscriptions[parsed.subject]++
                ws.emit(parsed.subject, msg)
            } else if (topic === '/sessionid') {
                ws.sessionId = parsed.id
                console.log('[websocket] got sessing id:', ws.sessionId)
            } else {
                console.error('no pending promise', message, topic, parsed)
            }
        }

        return new Promise((resolve, reject) => {
            ws.onopen = () => {
                this.retryCount[options.retryCount] = 0
                ws.emit(options.connected || 'server_connected')
                resolve(ws)
            }

            ws.onclose = () => {
                ws.emit(options.disconnected || 'server_disconnected')
                reject()
            }

            ws.onerror = (err) => {
                console.error(err)
                reject(err)
            }
        })
    }

}