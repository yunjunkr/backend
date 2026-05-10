--
-- PostgreSQL database dump
--

\restrict dJx3IgiugBwJZYMDWrR0US0pal6u35Hdm2fQbPsePO0N9aGn2WEodEMq8BSov0h

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: zoopick; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA zoopick;


ALTER SCHEMA zoopick OWNER TO postgres;

--
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA zoopick;


--
-- Name: EXTENSION vector; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';


--
-- Name: chat_message_type; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.chat_message_type AS ENUM (
    'USER',
    'SYSTEM'
);


ALTER TYPE zoopick.chat_message_type OWNER TO postgres;

--
-- Name: chat_room_status; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.chat_room_status AS ENUM (
    'OPEN',
    'RESOLVED_RETURNED',
    'RESOLVED_ABANDONED'
);


ALTER TYPE zoopick.chat_room_status OWNER TO postgres;

--
-- Name: day_of_week; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.day_of_week AS ENUM (
    'MON',
    'TUE',
    'WED',
    'THU',
    'FRI',
    'SAT',
    'SUN'
);


ALTER TYPE zoopick.day_of_week OWNER TO postgres;

--
-- Name: detection_review_status; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.detection_review_status AS ENUM (
    'PENDING',
    'CONFIRMED_SELF',
    'REJECTED_SELF',
    'UNCERTAIN'
);


ALTER TYPE zoopick.detection_review_status OWNER TO postgres;

--
-- Name: item_category; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.item_category AS ENUM (
    'SMARTPHONE',
    'EARPHONES',
    'BAG',
    'WALLET',
    'CREDIT_CARD',
    'STUDENT_ID_CARD',
    'TEXTBOOK',
    'NOTEBOOK',
    'UMBRELLA',
    'WATER_BOTTLE',
    'PENCIL_CASE',
    'PLUSH_TOY'
);


ALTER TYPE zoopick.item_category OWNER TO postgres;

--
-- Name: item_color; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.item_color AS ENUM (
    'BLACK',
    'WHITE',
    'GRAY',
    'RED',
    'BLUE',
    'GREEN',
    'YELLOW',
    'BROWN',
    'PINK',
    'PURPLE',
    'ORANGE',
    'BEIGE'
);


ALTER TYPE zoopick.item_color OWNER TO postgres;

--
-- Name: item_status; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.item_status AS ENUM (
    'REPORTED',
    'MATCHED',
    'IN_LOCKER',
    'IN_TRANSIT',
    'RETRIEVING',
    'RETURNED'
);


ALTER TYPE zoopick.item_status OWNER TO postgres;

--
-- Name: item_type; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.item_type AS ENUM (
    'LOST',
    'FOUND'
);


ALTER TYPE zoopick.item_type OWNER TO postgres;

--
-- Name: locker_command_status; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.locker_command_status AS ENUM (
    'PENDING',
    'CONSUMED',
    'COMPLETED'
);


ALTER TYPE zoopick.locker_command_status OWNER TO postgres;

--
-- Name: locker_command_type; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.locker_command_type AS ENUM (
    'OPEN',
    'CLOSE'
);


ALTER TYPE zoopick.locker_command_type OWNER TO postgres;

--
-- Name: locker_status; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.locker_status AS ENUM (
    'EMPTY',
    'IN_USE',
    'MAINTENANCE'
);


ALTER TYPE zoopick.locker_status OWNER TO postgres;

--
-- Name: match_status; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.match_status AS ENUM (
    'CANDIDATE',
    'NOTIFIED',
    'CONFIRMED',
    'REJECTED'
);


ALTER TYPE zoopick.match_status OWNER TO postgres;

--
-- Name: notification_type; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.notification_type AS ENUM (
    'MATCH_FOUND',
    'CHAT_MESSAGE',
    'ITEM_RETURNED',
    'THEFT_SUSPECTED',
    'LOCKER_READY'
);


ALTER TYPE zoopick.notification_type OWNER TO postgres;

--
-- Name: user_role; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.user_role AS ENUM (
    'STUDENT',
    'ADMIN'
);


ALTER TYPE zoopick.user_role OWNER TO postgres;

--
-- Name: video_analysis_status; Type: TYPE; Schema: zoopick; Owner: postgres
--

CREATE TYPE zoopick.video_analysis_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED'
);


ALTER TYPE zoopick.video_analysis_status OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: buildings; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.buildings (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(20) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL
);


ALTER TABLE zoopick.buildings OWNER TO postgres;

--
-- Name: buildings_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.buildings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.buildings_id_seq OWNER TO postgres;

--
-- Name: buildings_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.buildings_id_seq OWNED BY zoopick.buildings.id;


--
-- Name: cctv_detection_matches; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.cctv_detection_matches (
    id bigint NOT NULL,
    detection_id bigint NOT NULL,
    item_id bigint NOT NULL
);


ALTER TABLE zoopick.cctv_detection_matches OWNER TO postgres;

--
-- Name: cctv_detection_matches_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.cctv_detection_matches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.cctv_detection_matches_id_seq OWNER TO postgres;

--
-- Name: cctv_detection_matches_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.cctv_detection_matches_id_seq OWNED BY zoopick.cctv_detection_matches.id;


--
-- Name: cctv_detections; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.cctv_detections (
    id bigint NOT NULL,
    video_id bigint NOT NULL,
    detected_at timestamp without time zone NOT NULL,
    detected_category zoopick.item_category,
    detected_color zoopick.item_color,
    embedding zoopick.vector(512),
    item_snapshot_url character varying(500) NOT NULL,
    moment_snapshot_url character varying(500) NOT NULL,
    review_status zoopick.detection_review_status DEFAULT 'PENDING'::zoopick.detection_review_status NOT NULL,
    reviewed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE zoopick.cctv_detections OWNER TO postgres;

--
-- Name: cctv_detections_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.cctv_detections_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.cctv_detections_id_seq OWNER TO postgres;

--
-- Name: cctv_detections_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.cctv_detections_id_seq OWNED BY zoopick.cctv_detections.id;


--
-- Name: cctv_video_progress; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.cctv_video_progress (
    id bigint NOT NULL,
    video_id bigint NOT NULL,
    status zoopick.video_analysis_status DEFAULT 'PENDING'::zoopick.video_analysis_status NOT NULL,
    total_duration_seconds integer NOT NULL,
    analyzed_seconds integer DEFAULT 0 NOT NULL,
    estimated_completion_at timestamp without time zone,
    started_at timestamp without time zone,
    last_updated_at timestamp without time zone
);


ALTER TABLE zoopick.cctv_video_progress OWNER TO postgres;

--
-- Name: cctv_video_progress_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.cctv_video_progress_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.cctv_video_progress_id_seq OWNER TO postgres;

--
-- Name: cctv_video_progress_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.cctv_video_progress_id_seq OWNED BY zoopick.cctv_video_progress.id;


--
-- Name: cctv_videos; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.cctv_videos (
    id bigint NOT NULL,
    room_id bigint NOT NULL,
    recorded_at timestamp without time zone NOT NULL,
    duration_seconds integer NOT NULL,
    video_url character varying(500) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE zoopick.cctv_videos OWNER TO postgres;

--
-- Name: cctv_videos_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.cctv_videos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.cctv_videos_id_seq OWNER TO postgres;

--
-- Name: cctv_videos_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.cctv_videos_id_seq OWNED BY zoopick.cctv_videos.id;


--
-- Name: chat_messages; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.chat_messages (
    id bigint NOT NULL,
    room_id bigint NOT NULL,
    type zoopick.chat_message_type DEFAULT 'USER'::zoopick.chat_message_type NOT NULL,
    sender_id bigint,
    content text NOT NULL,
    read_at timestamp without time zone,
    sent_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_message_sender CHECK ((((type = 'USER'::zoopick.chat_message_type) AND (sender_id IS NOT NULL)) OR ((type = 'SYSTEM'::zoopick.chat_message_type) AND (sender_id IS NULL))))
);


ALTER TABLE zoopick.chat_messages OWNER TO postgres;

--
-- Name: chat_messages_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.chat_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.chat_messages_id_seq OWNER TO postgres;

--
-- Name: chat_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.chat_messages_id_seq OWNED BY zoopick.chat_messages.id;


--
-- Name: chat_rooms; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.chat_rooms (
    id bigint NOT NULL,
    item_id bigint,
    owner_id bigint NOT NULL,
    finder_id bigint NOT NULL,
    status zoopick.chat_room_status DEFAULT 'OPEN'::zoopick.chat_room_status NOT NULL,
    resolved_by bigint,
    resolved_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_chatrooms_resolved CHECK ((((status = 'OPEN'::zoopick.chat_room_status) AND (resolved_by IS NULL) AND (resolved_at IS NULL)) OR ((status = ANY (ARRAY['RESOLVED_RETURNED'::zoopick.chat_room_status, 'RESOLVED_ABANDONED'::zoopick.chat_room_status])) AND (resolved_at IS NOT NULL))))
);


ALTER TABLE zoopick.chat_rooms OWNER TO postgres;

--
-- Name: chat_rooms_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.chat_rooms_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.chat_rooms_id_seq OWNER TO postgres;

--
-- Name: chat_rooms_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.chat_rooms_id_seq OWNED BY zoopick.chat_rooms.id;


--
-- Name: course_schedules_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.course_schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.course_schedules_id_seq OWNER TO postgres;

--
-- Name: course_schedules; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.course_schedules (
    id bigint DEFAULT nextval('zoopick.course_schedules_id_seq'::regclass) NOT NULL,
    course_id bigint NOT NULL,
    day_of_week zoopick.day_of_week NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    CONSTRAINT chk_schedule_time CHECK ((start_time < end_time))
);


ALTER TABLE zoopick.course_schedules OWNER TO postgres;

--
-- Name: courses; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.courses (
    id bigint NOT NULL,
    course_name character varying(100) NOT NULL,
    room_id bigint NOT NULL,
    year integer NOT NULL,
    semester integer NOT NULL
);


ALTER TABLE zoopick.courses OWNER TO postgres;

--
-- Name: courses_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.courses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.courses_id_seq OWNER TO postgres;

--
-- Name: courses_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.courses_id_seq OWNED BY zoopick.courses.id;


--
-- Name: item_matches; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.item_matches (
    id bigint NOT NULL,
    lost_item_id bigint NOT NULL,
    found_item_id bigint NOT NULL,
    score real NOT NULL,
    status zoopick.match_status DEFAULT 'CANDIDATE'::zoopick.match_status NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE zoopick.item_matches OWNER TO postgres;

--
-- Name: item_matches_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.item_matches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.item_matches_id_seq OWNER TO postgres;

--
-- Name: item_matches_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.item_matches_id_seq OWNED BY zoopick.item_matches.id;


--
-- Name: item_posts; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.item_posts (
    id bigint NOT NULL,
    title character varying(512),
    description character varying(512),
    item_id bigint,
    user_id bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE zoopick.item_posts OWNER TO postgres;

--
-- Name: item_posts_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.item_posts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.item_posts_id_seq OWNER TO postgres;

--
-- Name: item_posts_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.item_posts_id_seq OWNED BY zoopick.item_posts.id;


--
-- Name: items; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.items (
    id bigint NOT NULL,
    reporter_id bigint NOT NULL,
    type zoopick.item_type NOT NULL,
    status zoopick.item_status DEFAULT 'REPORTED'::zoopick.item_status NOT NULL,
    category zoopick.item_category,
    color zoopick.item_color,
    embedding zoopick.vector(512),
    reported_building_id bigint,
    location_name character varying(255),
    reported_at timestamp without time zone,
    theft_suspected_at timestamp without time zone,
    returned_at timestamp without time zone,
    image_url character varying(500),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE zoopick.items OWNER TO postgres;

--
-- Name: items_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.items_id_seq OWNER TO postgres;

--
-- Name: items_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.items_id_seq OWNED BY zoopick.items.id;


--
-- Name: locker_commands; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.locker_commands (
    id bigint NOT NULL,
    locker_id bigint NOT NULL,
    command zoopick.locker_command_type NOT NULL,
    status zoopick.locker_command_status DEFAULT 'PENDING'::zoopick.locker_command_status NOT NULL,
    issued_by bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    consumed_at timestamp without time zone,
    completed_at timestamp without time zone
);


ALTER TABLE zoopick.locker_commands OWNER TO postgres;

--
-- Name: locker_commands_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.locker_commands_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.locker_commands_id_seq OWNER TO postgres;

--
-- Name: locker_commands_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.locker_commands_id_seq OWNED BY zoopick.locker_commands.id;


--
-- Name: lockers; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.lockers (
    id bigint NOT NULL,
    status zoopick.locker_status DEFAULT 'EMPTY'::zoopick.locker_status NOT NULL,
    current_item_id bigint
);


ALTER TABLE zoopick.lockers OWNER TO postgres;

--
-- Name: notifications; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.notifications (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    type zoopick.notification_type NOT NULL,
    payload jsonb,
    read_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE zoopick.notifications OWNER TO postgres;

--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.notifications_id_seq OWNER TO postgres;

--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.notifications_id_seq OWNED BY zoopick.notifications.id;


--
-- Name: rooms; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.rooms (
    id bigint NOT NULL,
    building_id bigint NOT NULL,
    name character varying(50) NOT NULL
);


ALTER TABLE zoopick.rooms OWNER TO postgres;

--
-- Name: rooms_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.rooms_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.rooms_id_seq OWNER TO postgres;

--
-- Name: rooms_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.rooms_id_seq OWNED BY zoopick.rooms.id;


--
-- Name: timetable_groups_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.timetable_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.timetable_groups_id_seq OWNER TO postgres;

--
-- Name: timetable_groups; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.timetable_groups (
    id bigint DEFAULT nextval('zoopick.timetable_groups_id_seq'::regclass) NOT NULL,
    user_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    year integer NOT NULL,
    semester integer NOT NULL,
    is_primary boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE zoopick.timetable_groups OWNER TO postgres;

--
-- Name: timetables; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.timetables (
    id bigint NOT NULL,
    course_id bigint NOT NULL,
    enrolled_at timestamp without time zone DEFAULT now() NOT NULL,
    timetable_group_id bigint NOT NULL,
    color character varying(7) DEFAULT '#3366FF'::character varying
);


ALTER TABLE zoopick.timetables OWNER TO postgres;

--
-- Name: timetables_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.timetables_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.timetables_id_seq OWNER TO postgres;

--
-- Name: timetables_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.timetables_id_seq OWNED BY zoopick.timetables.id;


--
-- Name: users; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.users (
    id bigint NOT NULL,
    school_email character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    nickname character varying(50) NOT NULL,
    department character varying(50) NOT NULL,
    grade character varying(20) NOT NULL,
    fcm_token character varying(512),
    role zoopick.user_role DEFAULT 'STUDENT'::zoopick.user_role NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone
);


ALTER TABLE zoopick.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: zoopick; Owner: postgres
--

CREATE SEQUENCE zoopick.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE zoopick.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: zoopick; Owner: postgres
--

ALTER SEQUENCE zoopick.users_id_seq OWNED BY zoopick.users.id;


--
-- Name: buildings id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.buildings ALTER COLUMN id SET DEFAULT nextval('zoopick.buildings_id_seq'::regclass);


--
-- Name: cctv_detection_matches id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detection_matches ALTER COLUMN id SET DEFAULT nextval('zoopick.cctv_detection_matches_id_seq'::regclass);


--
-- Name: cctv_detections id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detections ALTER COLUMN id SET DEFAULT nextval('zoopick.cctv_detections_id_seq'::regclass);


--
-- Name: cctv_video_progress id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_video_progress ALTER COLUMN id SET DEFAULT nextval('zoopick.cctv_video_progress_id_seq'::regclass);


--
-- Name: cctv_videos id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_videos ALTER COLUMN id SET DEFAULT nextval('zoopick.cctv_videos_id_seq'::regclass);


--
-- Name: chat_messages id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_messages ALTER COLUMN id SET DEFAULT nextval('zoopick.chat_messages_id_seq'::regclass);


--
-- Name: chat_rooms id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_rooms ALTER COLUMN id SET DEFAULT nextval('zoopick.chat_rooms_id_seq'::regclass);


--
-- Name: courses id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.courses ALTER COLUMN id SET DEFAULT nextval('zoopick.courses_id_seq'::regclass);


--
-- Name: item_matches id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_matches ALTER COLUMN id SET DEFAULT nextval('zoopick.item_matches_id_seq'::regclass);


--
-- Name: item_posts id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_posts ALTER COLUMN id SET DEFAULT nextval('zoopick.item_posts_id_seq'::regclass);


--
-- Name: items id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.items ALTER COLUMN id SET DEFAULT nextval('zoopick.items_id_seq'::regclass);


--
-- Name: locker_commands id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.locker_commands ALTER COLUMN id SET DEFAULT nextval('zoopick.locker_commands_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.notifications ALTER COLUMN id SET DEFAULT nextval('zoopick.notifications_id_seq'::regclass);


--
-- Name: rooms id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.rooms ALTER COLUMN id SET DEFAULT nextval('zoopick.rooms_id_seq'::regclass);


--
-- Name: timetables id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables ALTER COLUMN id SET DEFAULT nextval('zoopick.timetables_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.users ALTER COLUMN id SET DEFAULT nextval('zoopick.users_id_seq'::regclass);


--
-- Data for Name: buildings; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.buildings (id, name, code, latitude, longitude) FROM stdin;
1	제5공학관	ENG5	37.221984	127.187616
2	제2공학관	ENG2	37.221523	127.186795
3	제1공학관	ENG1	37.222482	127.187167
4	함박관	HBK	37.221122	127.18861
5	창조예술관	ART	37.222838	127.189279
\.


--
-- Data for Name: cctv_detection_matches; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.cctv_detection_matches (id, detection_id, item_id) FROM stdin;
\.


--
-- Data for Name: cctv_detections; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.cctv_detections (id, video_id, detected_at, detected_category, detected_color, embedding, item_snapshot_url, moment_snapshot_url, review_status, reviewed_at, created_at) FROM stdin;
\.


--
-- Data for Name: cctv_video_progress; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.cctv_video_progress (id, video_id, status, total_duration_seconds, analyzed_seconds, estimated_completion_at, started_at, last_updated_at) FROM stdin;
\.


--
-- Data for Name: cctv_videos; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.cctv_videos (id, room_id, recorded_at, duration_seconds, video_url, created_at) FROM stdin;
\.


--
-- Data for Name: chat_messages; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.chat_messages (id, room_id, type, sender_id, content, read_at, sent_at) FROM stdin;
\.


--
-- Data for Name: chat_rooms; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.chat_rooms (id, item_id, owner_id, finder_id, status, resolved_by, resolved_at, created_at) FROM stdin;
\.


--
-- Data for Name: course_schedules; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.course_schedules (id, course_id, day_of_week, start_time, end_time) FROM stdin;
27	1	MON	13:00:00	14:50:00
28	2	FRI	09:00:00	11:50:00
29	3	MON	13:00:00	14:50:00
30	3	WED	14:00:00	14:50:00
31	4	TUE	14:00:00	14:50:00
32	5	MON	10:00:00	12:50:00
33	6	THU	13:00:00	14:50:00
34	7	WED	10:00:00	11:50:00
35	8	TUE	14:00:00	16:50:00
36	9	MON	13:00:00	15:50:00
37	10	MON	10:00:00	11:50:00
38	10	WED	10:00:00	10:50:00
39	11	TUE	13:00:00	14:50:00
40	11	THU	13:00:00	13:50:00
41	12	THU	13:00:00	15:50:00
42	13	MON	13:00:00	14:50:00
43	13	WED	13:00:00	13:50:00
44	14	MON	14:00:00	15:50:00
45	14	WED	14:00:00	15:50:00
46	15	MON	11:00:00	12:50:00
47	15	WED	11:00:00	11:50:00
48	16	TUE	11:00:00	11:50:00
49	16	THU	11:00:00	11:50:00
50	17	TUE	11:00:00	13:50:00
51	18	TUE	13:00:00	14:50:00
52	19	FRI	13:00:00	15:50:00
\.


--
-- Data for Name: courses; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.courses (id, course_name, room_id, year, semester) FROM stdin;
1	4차산업혁명과미래사회진로선택	1	2026	1
2	공학수학1	2	2026	1
3	글쓰기	3	2026	1
4	환경과인간	4	2026	1
5	통계학	5	2026	1
6	일반생물학	6	2026	1
7	4차산업혁명과미래사회진로선택	7	2026	1
8	컴퓨터하드웨어	7	2026	1
9	캡스톤디자인	8	2026	1
10	운영체제	9	2026	1
11	공학수학1	9	2026	1
12	시스템클라우드보안	10	2026	1
13	기계학습	10	2026	1
14	발표와토의	11	2026	1
15	미적분학1	12	2026	1
16	영어1	13	2026	1
17	세계영화사	14	2026	1
18	교양바둑	15	2026	1
19	뮤지컬개론	16	2026	1
\.


--
-- Data for Name: item_matches; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.item_matches (id, lost_item_id, found_item_id, score, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: item_posts; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.item_posts (id, title, description, item_id, user_id, created_at) FROM stdin;
\.


--
-- Data for Name: items; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.items (id, reporter_id, type, status, category, color, embedding, reported_building_id, location_name, reported_at, theft_suspected_at, returned_at, image_url, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: locker_commands; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.locker_commands (id, locker_id, command, status, issued_by, created_at, consumed_at, completed_at) FROM stdin;
\.


--
-- Data for Name: lockers; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.lockers (id, status, current_item_id) FROM stdin;
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.notifications (id, user_id, type, payload, read_at, created_at) FROM stdin;
\.


--
-- Data for Name: rooms; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.rooms (id, building_id, name) FROM stdin;
1	3	Y106
2	3	Y123
3	3	Y117
4	2	Y8110
5	2	Y8107
6	2	Y8114
7	1	Y5420
8	1	Y5441
9	1	Y5411
10	1	Y5445
11	4	Y9501
12	4	Y9508
13	4	Y9515
14	5	Y2237
15	5	Y2259
16	5	Y2119
\.


--
-- Data for Name: timetable_groups; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.timetable_groups (id, user_id, name, year, semester, is_primary, created_at) FROM stdin;
\.


--
-- Data for Name: timetables; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.timetables (id, course_id, enrolled_at, timetable_group_id, color) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.users (id, school_email, password, nickname, department, grade, fcm_token, role, created_at, updated_at) FROM stdin;
\.


--
-- Name: buildings_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.buildings_id_seq', 5, true);


--
-- Name: cctv_detection_matches_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.cctv_detection_matches_id_seq', 1, false);


--
-- Name: cctv_detections_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.cctv_detections_id_seq', 1, false);


--
-- Name: cctv_video_progress_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.cctv_video_progress_id_seq', 1, false);


--
-- Name: cctv_videos_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.cctv_videos_id_seq', 1, false);


--
-- Name: chat_messages_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.chat_messages_id_seq', 1, false);


--
-- Name: chat_rooms_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.chat_rooms_id_seq', 1, false);


--
-- Name: course_schedules_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.course_schedules_id_seq', 52, true);


--
-- Name: courses_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.courses_id_seq', 19, true);


--
-- Name: item_matches_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.item_matches_id_seq', 1, false);


--
-- Name: item_posts_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.item_posts_id_seq', 1, false);


--
-- Name: items_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.items_id_seq', 1, false);


--
-- Name: locker_commands_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.locker_commands_id_seq', 1, false);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.notifications_id_seq', 1, false);


--
-- Name: rooms_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.rooms_id_seq', 16, true);


--
-- Name: timetable_groups_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.timetable_groups_id_seq', 1, false);


--
-- Name: timetables_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.timetables_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.users_id_seq', 1, false);


--
-- Name: buildings buildings_code_key; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.buildings
    ADD CONSTRAINT buildings_code_key UNIQUE (code);


--
-- Name: buildings buildings_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.buildings
    ADD CONSTRAINT buildings_pkey PRIMARY KEY (id);


--
-- Name: cctv_detection_matches cctv_detection_matches_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detection_matches
    ADD CONSTRAINT cctv_detection_matches_pkey PRIMARY KEY (id);


--
-- Name: cctv_detections cctv_detections_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detections
    ADD CONSTRAINT cctv_detections_pkey PRIMARY KEY (id);


--
-- Name: cctv_video_progress cctv_video_progress_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_video_progress
    ADD CONSTRAINT cctv_video_progress_pkey PRIMARY KEY (id);


--
-- Name: cctv_video_progress cctv_video_progress_video_id_key; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_video_progress
    ADD CONSTRAINT cctv_video_progress_video_id_key UNIQUE (video_id);


--
-- Name: cctv_videos cctv_videos_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_videos
    ADD CONSTRAINT cctv_videos_pkey PRIMARY KEY (id);


--
-- Name: chat_messages chat_messages_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_messages
    ADD CONSTRAINT chat_messages_pkey PRIMARY KEY (id);


--
-- Name: chat_rooms chat_rooms_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_rooms
    ADD CONSTRAINT chat_rooms_pkey PRIMARY KEY (id);


--
-- Name: course_schedules course_schedules_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.course_schedules
    ADD CONSTRAINT course_schedules_pkey PRIMARY KEY (id);


--
-- Name: courses courses_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);


--
-- Name: item_matches item_matches_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_matches
    ADD CONSTRAINT item_matches_pkey PRIMARY KEY (id);


--
-- Name: item_posts item_posts_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_posts
    ADD CONSTRAINT item_posts_pkey PRIMARY KEY (id);


--
-- Name: items items_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.items
    ADD CONSTRAINT items_pkey PRIMARY KEY (id);


--
-- Name: locker_commands locker_commands_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.locker_commands
    ADD CONSTRAINT locker_commands_pkey PRIMARY KEY (id);


--
-- Name: lockers lockers_current_item_id_key; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.lockers
    ADD CONSTRAINT lockers_current_item_id_key UNIQUE (current_item_id);


--
-- Name: lockers lockers_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.lockers
    ADD CONSTRAINT lockers_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: rooms rooms_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.rooms
    ADD CONSTRAINT rooms_pkey PRIMARY KEY (id);


--
-- Name: timetable_groups timetable_groups_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetable_groups
    ADD CONSTRAINT timetable_groups_pkey PRIMARY KEY (id);


--
-- Name: timetables timetables_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT timetables_pkey PRIMARY KEY (id);


--
-- Name: courses uq_course_per_semester; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.courses
    ADD CONSTRAINT uq_course_per_semester UNIQUE (room_id, year, semester, course_name);


--
-- Name: course_schedules uq_course_schedule; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.course_schedules
    ADD CONSTRAINT uq_course_schedule UNIQUE (course_id, day_of_week, start_time);


--
-- Name: cctv_detection_matches uq_detection_matches; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detection_matches
    ADD CONSTRAINT uq_detection_matches UNIQUE (detection_id, item_id);


--
-- Name: timetables uq_group_course; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT uq_group_course UNIQUE (timetable_group_id, course_id);


--
-- Name: item_matches uq_matches_pair; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_matches
    ADD CONSTRAINT uq_matches_pair UNIQUE (lost_item_id, found_item_id);


--
-- Name: rooms uq_rooms_building_name; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.rooms
    ADD CONSTRAINT uq_rooms_building_name UNIQUE (building_id, name);


--
-- Name: users uq_user_email; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.users
    ADD CONSTRAINT uq_user_email UNIQUE (school_email);


--
-- Name: timetable_groups uq_user_semester_name; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetable_groups
    ADD CONSTRAINT uq_user_semester_name UNIQUE (user_id, year, semester, name);


--
-- Name: users uq_users_nickname; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.users
    ADD CONSTRAINT uq_users_nickname UNIQUE (nickname);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_school_email_key; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.users
    ADD CONSTRAINT users_school_email_key UNIQUE (school_email);


--
-- Name: idx_items_embedding_hnsw; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_items_embedding_hnsw ON zoopick.items USING hnsw (embedding vector_cosine_ops);


--
-- Name: idx_detections_embedding_hnsw; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_detections_embedding_hnsw ON zoopick.cctv_detections USING hnsw (embedding vector_cosine_ops);


--
-- Name: idx_items_filtering; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_items_filtering ON zoopick.items (category, color);


--
-- Name: idx_chatrooms_open; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_chatrooms_open ON zoopick.chat_rooms USING btree (status) WHERE (status = 'OPEN'::zoopick.chat_room_status);


--
-- Name: idx_commands_pending; Type: INDEX; Schema: zoopick; Owner: postgres
--


CREATE INDEX idx_commands_pending ON zoopick.locker_commands USING btree (locker_id, created_at) WHERE (status = 'PENDING'::zoopick.locker_command_status);


--
-- Name: idx_course_schedules_course; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_course_schedules_course ON zoopick.course_schedules USING btree (course_id);


--
-- Name: idx_course_schedules_dow_time; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_course_schedules_dow_time ON zoopick.course_schedules USING btree (day_of_week, start_time, end_time);


--
-- Name: idx_courses_room; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_courses_room ON zoopick.courses USING btree (room_id);


--
-- Name: idx_courses_semester; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_courses_semester ON zoopick.courses USING btree (year, semester);


--
-- Name: idx_detections_pending; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_detections_pending ON zoopick.cctv_detections USING btree (review_status) WHERE (review_status = 'PENDING'::zoopick.detection_review_status);


--
-- Name: idx_detections_video_time; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_detections_video_time ON zoopick.cctv_detections USING btree (video_id, detected_at);


--
-- Name: idx_items_building; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_items_building ON zoopick.items USING btree (reported_building_id);


--
-- Name: idx_items_created; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_items_created ON zoopick.items USING btree (created_at DESC);


--
-- Name: idx_items_filtering; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_items_filtering ON zoopick.items USING btree (category, color);


--
-- Name: idx_items_reporter; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_items_reporter ON zoopick.items USING btree (reporter_id);


--
-- Name: idx_items_type_status; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_items_type_status ON zoopick.items USING btree (type, status);


--
-- Name: idx_matches_lost_status; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_matches_lost_status ON zoopick.item_matches USING btree (lost_item_id, status);


--
-- Name: idx_messages_room_time; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_messages_room_time ON zoopick.chat_messages USING btree (room_id, sent_at DESC);


--
-- Name: idx_notifications_user_created; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_notifications_user_created ON zoopick.notifications USING btree (user_id, created_at DESC);


--
-- Name: idx_notifications_user_unread; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_notifications_user_unread ON zoopick.notifications USING btree (user_id) WHERE (read_at IS NULL);


--
-- Name: idx_timetables_course; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_timetables_course ON zoopick.timetables USING btree (course_id);


--
-- Name: idx_timetables_group_id; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_timetables_group_id ON zoopick.timetables USING btree (timetable_group_id);


--
-- Name: idx_video_progress_status_time; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_video_progress_status_time ON zoopick.cctv_video_progress USING btree (status, last_updated_at) WHERE (status = ANY (ARRAY['PENDING'::zoopick.video_analysis_status, 'IN_PROGRESS'::zoopick.video_analysis_status]));


--
-- Name: idx_videos_room_time; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_videos_room_time ON zoopick.cctv_videos USING btree (room_id, recorded_at DESC);


--
-- Name: uq_chatrooms_with_item; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE UNIQUE INDEX uq_chatrooms_with_item ON zoopick.chat_rooms USING btree (item_id, owner_id, finder_id) WHERE (item_id IS NOT NULL);


--
-- Name: uq_chatrooms_without_item; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE UNIQUE INDEX uq_chatrooms_without_item ON zoopick.chat_rooms USING btree (owner_id, finder_id) WHERE (item_id IS NULL);


--
-- Name: chat_rooms fk_chatrooms_finder; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_rooms
    ADD CONSTRAINT fk_chatrooms_finder FOREIGN KEY (finder_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: chat_rooms fk_chatrooms_item; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_rooms
    ADD CONSTRAINT fk_chatrooms_item FOREIGN KEY (item_id) REFERENCES zoopick.items(id) ON DELETE SET NULL;


--
-- Name: chat_rooms fk_chatrooms_owner; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_rooms
    ADD CONSTRAINT fk_chatrooms_owner FOREIGN KEY (owner_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: chat_rooms fk_chatrooms_resolver; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_rooms
    ADD CONSTRAINT fk_chatrooms_resolver FOREIGN KEY (resolved_by) REFERENCES zoopick.users(id) ON DELETE SET NULL;


--
-- Name: locker_commands fk_commands_issuer; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.locker_commands
    ADD CONSTRAINT fk_commands_issuer FOREIGN KEY (issued_by) REFERENCES zoopick.users(id) ON DELETE SET NULL;


--
-- Name: locker_commands fk_commands_locker; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.locker_commands
    ADD CONSTRAINT fk_commands_locker FOREIGN KEY (locker_id) REFERENCES zoopick.lockers(id) ON DELETE CASCADE;


--
-- Name: courses fk_courses_room; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.courses
    ADD CONSTRAINT fk_courses_room FOREIGN KEY (room_id) REFERENCES zoopick.rooms(id) ON DELETE CASCADE;


--
-- Name: cctv_detection_matches fk_detection_matches_detection; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detection_matches
    ADD CONSTRAINT fk_detection_matches_detection FOREIGN KEY (detection_id) REFERENCES zoopick.cctv_detections(id) ON DELETE CASCADE;


--
-- Name: cctv_detection_matches fk_detection_matches_item; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detection_matches
    ADD CONSTRAINT fk_detection_matches_item FOREIGN KEY (item_id) REFERENCES zoopick.items(id) ON DELETE CASCADE;


--
-- Name: cctv_detections fk_detections_video; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detections
    ADD CONSTRAINT fk_detections_video FOREIGN KEY (video_id) REFERENCES zoopick.cctv_videos(id) ON DELETE CASCADE;


--
-- Name: item_posts fk_item_posts_item; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_posts
    ADD CONSTRAINT fk_item_posts_item FOREIGN KEY (item_id) REFERENCES zoopick.items(id) ON DELETE CASCADE;


--
-- Name: item_posts fk_item_posts_user; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_posts
    ADD CONSTRAINT fk_item_posts_user FOREIGN KEY (user_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: items fk_items_building; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.items
    ADD CONSTRAINT fk_items_building FOREIGN KEY (reported_building_id) REFERENCES zoopick.buildings(id) ON DELETE SET NULL;


--
-- Name: items fk_items_reporter; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.items
    ADD CONSTRAINT fk_items_reporter FOREIGN KEY (reporter_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: lockers fk_lockers_item; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.lockers
    ADD CONSTRAINT fk_lockers_item FOREIGN KEY (current_item_id) REFERENCES zoopick.items(id) ON DELETE SET NULL;


--
-- Name: item_matches fk_matches_found; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_matches
    ADD CONSTRAINT fk_matches_found FOREIGN KEY (found_item_id) REFERENCES zoopick.items(id) ON DELETE CASCADE;


--
-- Name: item_matches fk_matches_lost; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.item_matches
    ADD CONSTRAINT fk_matches_lost FOREIGN KEY (lost_item_id) REFERENCES zoopick.items(id) ON DELETE CASCADE;


--
-- Name: chat_messages fk_messages_room; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_messages
    ADD CONSTRAINT fk_messages_room FOREIGN KEY (room_id) REFERENCES zoopick.chat_rooms(id) ON DELETE CASCADE;


--
-- Name: chat_messages fk_messages_sender; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.chat_messages
    ADD CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: notifications fk_notifications_user; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.notifications
    ADD CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: cctv_video_progress fk_progress_video; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_video_progress
    ADD CONSTRAINT fk_progress_video FOREIGN KEY (video_id) REFERENCES zoopick.cctv_videos(id) ON DELETE CASCADE;


--
-- Name: rooms fk_rooms_building; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.rooms
    ADD CONSTRAINT fk_rooms_building FOREIGN KEY (building_id) REFERENCES zoopick.buildings(id) ON DELETE CASCADE;


--
-- Name: timetable_groups fk_timetable_groups_user; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetable_groups
    ADD CONSTRAINT fk_timetable_groups_user FOREIGN KEY (user_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: timetables fk_timetables_group; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT fk_timetables_group FOREIGN KEY (timetable_group_id) REFERENCES zoopick.timetable_groups(id) ON DELETE CASCADE;


--
-- Name: timetables fk_user_courses_course; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT fk_user_courses_course FOREIGN KEY (course_id) REFERENCES zoopick.courses(id) ON DELETE CASCADE;


--
-- Name: cctv_videos fk_videos_room; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_videos
    ADD CONSTRAINT fk_videos_room FOREIGN KEY (room_id) REFERENCES zoopick.rooms(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict dJx3IgiugBwJZYMDWrR0US0pal6u35Hdm2fQbPsePO0N9aGn2WEodEMq8BSov0h

