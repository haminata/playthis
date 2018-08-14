
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
        this.state.searchQuerySong = null
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
        app.musicroomView = this
        //app.setState({newMusicroom: this})
        //document.getElementById("form_create_musicroom").classList.remove('hidden')
        if (this.isNew) app.getJson("/db/users").then((res) => {
            console.log('[load users]', res)
            this.setState({users: res.data || []})
        })

        if(this.viewFormat === VIEW_FORMAT.FULL && !this.state.editMode) {
            app.getJson("/db/songs").then((res) => {
                console.log('[load songs]', res)
                this.setState({songs: toCamelCase(res.data || [])})
            })
        }
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

    renderGridItem(){
        const style = {
            backgroundColor: this.name,
            maxHeight: '20vh',
            marginTop: '16px'
        };
        let key = `${this.constructor.name}_${this.state.id}`;

        let onJoinClick = () => {
            app.selectedRoom = this.state;
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

    onTrackIdChange(event){
        this.trackId = event.target.value.trim();
    }

    renderView(){
        let songs = _.map(this.state.songs || [], (s) => {
            return _.merge(s, {viewFormat: VIEW_FORMAT.LIST_ITEM});
        })

        if(this.state.searchQuerySong){
            let term = this.state.searchQuerySong.toLowerCase()
            songs = _.filter(songs, (song) => {
                return _.includes(((song.title || '') + (song.artistName || '')).toLowerCase(), term)
            })
        }

        let style = {width: '100px'}
        return e.div({className: 'container', style: {}}, [
            e.div({className: 'row'}, [
                e.div({className: "col-md-2"}, "Now Playing..."),
                e.div({className: "col-md-6"}, e.input({placeholder: 'Enter Spotify track', className: 'form-control', onChange: this.onTrackIdChange})),
                e.div({className: "col-md-2"}, e.button({style, className: 'btn btn-success', onClick: () => app.play(this.trackId)}, 'Play')),
                e.div({className: "col-md-2"}, e.button({style, className: 'btn btn-dark', onClick: () => app.pause(this.trackId)}, 'Pause')),
            ]),
            e.br(),
            e(ModelCollection, {modelsProps: songs, model: Song, viewFormat: VIEW_FORMAT.LIST_ITEM})
        ])
    }

    render() {
        if (this.state.editMode) return this.renderEditor()

        if (this.viewFormat === VIEW_FORMAT.GRID_ITEM) return this.renderGridItem()
        else if (this.viewFormat === VIEW_FORMAT.FULL) return this.renderView()
        return super.render()
    }
}

class Song extends DbModel {

    render(){

        return e.div({}, [
            e.img({src: this.state.thumbnailUrl, alt: this.state.title, className: 'rounded float-left', height: '80px', width: '80px', marginRight: '8px'}),
            e.h3({style: {marginLeft: '100px'}}, [`${this.state.title}`,
                e.small({className: 'text-muted', style: {marginLeft: '4px'}}, `by ${this.state.artistName}`)
            ]),

        ])
    }
}