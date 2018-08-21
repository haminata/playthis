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
        this.state.tracks = []
        this.container = React.createRef();
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

            if (name === 'created_by') r[name] = _.toInteger(v.value) // hack
        }, {id: this.state.id})
    }

    onPublish() {
        let data = this.data
        app.server.json('/create_room', data, {busy: true})
            .then((response) => {
                console.log('[create_room] ', response)
                let existing = _.find(app.state.musicrooms, {id: response.data.id})

                if (_.isObject(existing)) _.merge(existing, response.data)
                else app.state.musicrooms.unshift(response.data)

                app.setState({musicrooms: app.state.musicrooms})

                app.newMusicroom = null;
                app.selectedRoom = null;
            })
    }

    componentDidMount() {
        app.musicroomView = this
        //app.setState({newMusicroom: this})
        //document.getElementById("form_create_musicroom").classList.remove('hidden')
        if (this.isNew || this.state.editMode) app.getJson("/db/users").then((res) => {
            console.log('[load users]', res)
            this.setState({users: res.data || []})
        })

        if (this.viewFormat === VIEW_FORMAT.FULL && !this.state.editMode && !this.isNew) {
            app.server.json("/get_room_tracks", {roomId: this.state.id})
                .then((res) => {
                    console.log('[got tracks]', res)
                    this.setState({tracks: toCamelCase(res.data.tracks || [])})
                })
        }

        $(this.container.current).on('focusin', () => {
            console.log('[focus#musicroom]')
            $(app.toolbar.dropdown.current).hide()
        })
    }

    onCancel() {
        app.newMusicroom = null
        app.selectedRoom = null
    }

    addTrack(track) {
        console.log('[adding track]', track);
        app.server.json('/get_or_create_track', {track})
            .then((res) => {
                console.log('[get_or_create_track]', res);
                let track = res.data.track
                let newTracks = _.concat(this.state.tracks, [toCamelCase(track)])
                this.setState({tracks: newTracks})
                return app.server.json('/add_room_track', {roomId: this.state.id, trackId: track.id})
            })
            .then((res) => {
                console.log('[music#addTrack] track added:', res)
            })
    }

    onUserAdminChange(event) {
        this.setState({value: event.target.value});
    }

    onDescriptionChange(event) {
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
                        defaultValue: this.state.name,
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
                        defaultValue: this.state.description || '',
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
                        }, this.state.id ? 'Update' : 'Publish')
                    ]),
                    e.div({className: 'col-sm-2'}, [
                        e.button({
                            type: 'button',
                            className: 'btn btn-link text-danger pull-left',
                            onClick: this.onCancel.bind(this)
                        }, 'Cancel')
                    ])
                ])
            ])
        ])
    }

    renderGridItem() {
        const style = {
            backgroundColor: this.name,
            //maxHeight: '20vh',
            marginTop: '16px'
        };
        let key = `${this.constructor.name}_${this.state.id}`;

        let onJoinClick = () => {
            app.selectedRoom = _.merge(this.state, {viewFormat: VIEW_FORMAT.FULL, editMode: false});
        };

        let onEditClick = () => {
            app.selectedRoom = _.merge(this.state, {viewFormat: VIEW_FORMAT.FULL, editMode: true});
        }

        let onDeleteRoom = () => {
            app.server.json("/delete_room", Object.assign({roomId: this.state.id}, this.state))
                .then((res) => {
                    let rooms = app.state.musicrooms
                    console.log('[/delete_room] response:', res, rooms.length)
                    _.remove(rooms, (r) => r.id === this.state.id)
                    app.setState({musicrooms: rooms})
                    console.log('[/rooms] response:', rooms.length)
                })
        }

        let className = 'card shadow'
        let joinTxt = 'text-warning'

        if(!this.state.createdBy){
            className += ' bg-secondary text-white'
        }else if(this.state.id <= 24 && this.state.id >= 16){
            className += ' text-white bg-info'
        }else if(this.state.name === 'Test card'){
            className += ' text-white bg-warning'
            joinTxt = 'text-light'
        }else{
            className += ' text-white bg-success'
        }

        return e.div({style, key, className}, [
            //e.img({className: 'card-img-top', alt: 'Card image cap', src: ".../100px180/"}),
            //e.div({className: 'card-header'}, this.props.name || 'No header'),
            e.div({className: 'card-body show-on-hover-parent'}, [
                //<button type="button" class="close" aria-label="Close">
                //<span aria-hidden="true">&times;</span>
                //</button>

                e.button({
                    type: 'button',
                    onClick: onDeleteRoom,
                    className: 'close show-on-hover'
                }, e.span({dangerouslySetInnerHTML: {__html: '&times;'}})),
                e.h5({className: 'card-title', key}, this.state.name || 'No title'),
                e.h6({className: 'card-subtitle mb-2 text-black-50'}, `By ${this.createdBy.name}`),
                e.p({className: 'card-text'}, this.state.description || 'No description'),
                e.a({className: 'card-link ' + joinTxt, href: '#', onClick: onJoinClick}, 'Join'),
                e.a({className: 'card-link text-light', href: '#', onClick: onEditClick}, 'Edit'),
            ])
        ])
    }

    onTrackIdChange(event) {
        this.trackId = event.target.value.trim();
        this.setState({trackId: this.trackId})
    }

    renderView() {
        let songs = _.map(this.state.tracks || [], (s) => {
            return _.merge(s, {viewFormat: VIEW_FORMAT.LIST_ITEM});
        })

        let style = {width: '100px'}
        return e.div({className: 'music-play'}, [

            e.div({className: 'container', tabIndex: 0, ref: this.container, style: {outline: 'none', height: '100%'}}, [
                e.div({className: 'row', style: {'width': '100%', marginRight: 0}}, [
                    e.div({className: "col text-light"}, e.h4({className: 'mt-3'}, `Now Playing...${this.state.nowPlayingText || ''}`)),
                    e.button({className: 'btn btn-warning pull-right my-2', style: {width: '150px', marginRight: '-15px'}, type: "button"}, [e.i({className: 'fa fa-picture-o'}), ' Upload Photo']),

                    // e.div({className: "col-md-6"}, e.input({value: this.trackId, placeholder: 'Enter Spotify track', className: 'form-control', onChange: this.onTrackIdChange.bind(this)})),
                    // e.div({className: "col-md-2"}, e.button({style, className: 'btn btn-success', onClick: () => app.play(this.trackId)}, 'Play')),
                    // e.div({className: "col-md-2"}, e.button({style, className: 'btn btn-dark pull-left', onClick: () => app.pause(this.trackId)}, 'Pause')),
                ])]),

            e.div({id: "carouselExampleIndicators", style: {maxHeight: '50vh', overflow: 'hidden', position: 'relative'}, className:"carousel slide bg-dark", 'data-ride': "carousel"}, [
                e.div({className: 'carousel-indicators'}, [
                    e.li({className: 'active', 'data-target': "#carouselExampleIndicators", 'data-slide-to':"0"}),
                    e.li({className: '', 'data-target': "#carouselExampleIndicators", 'data-slide-to':"1"}),
                ]),
                e.div({className: 'carousel-inner', style: {width: '100%'}}, [
                    e.div({className: 'carousel-item active text-center', }, [
                        e.img({src: 'http://houseparty.com/assets/img/og-image.jpg',className:'shadow-lg' , alt: 'House Party'}),
                        e.div({className: 'carousel-caption d-none d-md-block'}, [
                            e.h5({}, 'A Nice Header'),
                            e.p({}, 'Some text')
                        ]),
                    ]),
                    e.div({className: 'carousel-item text-center', }, [
                        e.img({className:'shadow-lg', src: 'https://cached.imagescaler.hbpl.co.uk/resize/scaleWidth/743/cached.offlinehbpl.hbpl.co.uk/news/OMC/spotify-640-20140402112233111.jpg', alt: 'House Party'}),
                        e.div({className: 'carousel-caption d-none d-md-block'}, [
                            e.h5({}, 'A Nice Header'),
                            e.p({}, 'Some text')
                        ]),
                    ]),

                ]),
                e.a({className: 'carousel-control-prev', href: '#carouselExampleIndicators', 'data-slide': 'prev'}, [
                    e.span({className: 'carousel-control-prev-icon'}, []),
                    e.span({className: 'sr-only'}, 'Previous')
                ]),
                e.a({className: 'carousel-control-next', href: '#carouselExampleIndicators', 'data-slide': 'next'}, [
                    e.span({className: 'carousel-control-next-icon'}, []),
                    e.span({className: 'sr-only'}, 'Next')
                ])
            ]),

            e.br(),
            e.div({className: 'container', style: {overflow: 'scroll', minHeight: '50vh'}}, [
                e(ModelCollection, {modelsProps: songs, model: Track, viewFormat: VIEW_FORMAT.LIST_ITEM, className: 'rounded border my-3 bg-white '}),
            ])
        ])
    }

    render() {
        if (this.state.editMode) return this.renderEditor()

        if (this.viewFormat === VIEW_FORMAT.GRID_ITEM) return this.renderGridItem()
        else if (this.viewFormat === VIEW_FORMAT.FULL) return this.renderView()
        return super.render()
    }
}

class Track extends DbModel {

    isAddedToRoom() {
        let existing = _.find(app.musicroomView.state.tracks, {trackId: this.state.trackId})
        return _.isObject(existing)
    }

    render() {

        let onClick = () => {
            let uri = this.state.uri || this.state.trackId
            if (!uri) return
            app.play(uri)
            app.musicroomView.setState({tradeId: uri})
            app.musicroomView.setState({nowPlayingText: this.state.title})
        }

        let onPause = () => {
            let uri = this.state.uri || this.state.trackId
            if (!uri) return
            app.pause(uri)
            app.musicroomView.setState({tradeId: uri})
            app.musicroomView.setState({nowPlayingText: this.state.title + " (PAUSED)"})
        }

        let onAddTrack = () => {
            app.musicroomView.addTrack(this.state);
        }


        let addBtnStyle = {display: this.isAddedToRoom() ? 'none' : 'block'}

        let children = []
        if(app.state.nowPlayingText === this.title || !app.state.nowPlayingText){
            children.push(e.span({className: 'mr-2'}, 'Play'))
            children.push(e.i({className: 'fa fa-play-circle'}))
        }

        return e.div({}, [
            e.button({className: 'btn btn-outline-success pull-right', style: addBtnStyle, onClick: onAddTrack}, 'Add'),
            e.button({className: 'btn btn-link btn-lg text-secondary pull-right', style: {}, onClick: onPause}, 'Pause'),

            e.button({className: 'btn btn-link btn-lg text-primary pull-right', onClick: onClick},
                children
            ),

            e.img({
                src: this.state.thumbnailUrl,
                alt: this.state.title,
                className: 'rounded float-left',
                height: '80px',
                width: '80px',
                marginRight: '8px'
            }),
            e.h3({style: {marginLeft: '100px'}}, [`${this.state.title}`,
                e.small({className: 'text-muted', style: {marginLeft: '4px'}}, `by ${this.state.artistName}`)
            ]),
        ])
    }
}