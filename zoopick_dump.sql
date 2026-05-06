--
-- PostgreSQL database dump
--

\restrict zaaZzBLaYPZ6eE7GVrQDofgIBJxSzOfI7E2VSnwefDtdveDFbB7ctnhSUSWhGIv

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
-- Name: courses; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.courses (
                                 id bigint NOT NULL,
                                 course_name character varying(100) NOT NULL,
                                 room_id bigint NOT NULL,
                                 year integer NOT NULL,
                                 semester integer NOT NULL,
                                 day_of_week zoopick.day_of_week NOT NULL,
                                 start_time time without time zone NOT NULL,
                                 end_time time without time zone NOT NULL
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
                                      score_category real,
                                      score_visual real,
                                      score_spatial real,
                                      score_temporal real,
                                      score_total real NOT NULL,
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
-- Name: timetables; Type: TABLE; Schema: zoopick; Owner: postgres
--

CREATE TABLE zoopick.timetables (
                                    id bigint NOT NULL,
                                    user_id bigint NOT NULL,
                                    course_id bigint NOT NULL,
                                    enrolled_at timestamp without time zone DEFAULT now() NOT NULL
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
1	제5공학관	ENG5	37.2236	127.1878
2	제1공학관	ENG1	37.224	127.1882
3	함박관	HBK	37.2233	127.1872
4	차세대과학관	NSCI	37.2228	127.1885
5	창조예술관	ART	37.1232	127.321321
6	제2공학관	ENG2	37.2245	127.189
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
-- Data for Name: courses; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.courses (id, course_name, room_id, year, semester, day_of_week, start_time, end_time) FROM stdin;
1	캡스톤디자인1	1	2026	1	MON	09:00:00	12:00:00
2	운영체제	2	2026	1	MON	13:30:00	15:00:00
3	컴퓨터네트워크	3	2026	1	TUE	10:30:00	12:00:00
4	인공지능개론	1	2026	1	WED	13:30:00	16:30:00
5	데이터베이스	2	2026	1	THU	09:00:00	10:30:00
6	소프트웨어공학	3	2026	1	FRI	13:30:00	15:00:00
7	알고리즘	1	2026	1	MON	15:00:00	16:30:00
8	자료구조	2	2026	1	TUE	13:30:00	15:00:00
9	미적분학	6	2026	1	MON	09:00:00	10:30:00
10	일반물리	5	2026	1	TUE	15:00:00	16:30:00
11	교양영어	6	2026	1	WED	13:30:00	15:00:00
12	대학국어	4	2026	1	FRI	09:00:00	10:30:00
\.


--
-- Data for Name: item_matches; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.item_matches (id, lost_item_id, found_item_id, score_category, score_visual, score_spatial, score_temporal, score_total, status, created_at, updated_at) FROM stdin;
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
0	1	LOST	REPORTED	\N	\N	\N	1	5공학관 어디	2026-05-06 01:11:08.911	2026-05-06 01:11:31.039	2026-05-06 01:11:33.44	\N	2026-05-06 01:11:49.298	\N
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
1	EMPTY	\N
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
1	1	Y5407
2	1	Y5301
3	2	Y9029
4	3	Y22217
5	4	대강당
6	3	열람실1
\.


--
-- Data for Name: timetables; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.timetables (id, user_id, course_id, enrolled_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: zoopick; Owner: postgres
--

COPY zoopick.users (id, school_email, password, nickname, department, grade, fcm_token, role, created_at, updated_at) FROM stdin;
1	test@mju.ac.kr	$2a$10$dummyhashedpassword1234567890	테스트학생	컴퓨터공학과	4학년	\N	STUDENT	2026-05-03 20:49:32.934927	\N
2	admin@mju.ac.kr	$2a$10$dummyhashedpassword0987654321	관리자	시스템운영	0학년	\N	ADMIN	2026-05-03 20:49:32.934927	\N
5	soshat@mju.ac.kr	$2a$10$5ljJyHzJb8xGdFTfeMkV4exQ0xgNR73.YAFIPtGwmBrCShbVCfyfq	테스트1	컴퓨터공학전공	4학년	\N	STUDENT	2026-05-07 00:56:25.441323	\N
\.


--
-- Name: buildings_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.buildings_id_seq', 6, true);


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
-- Name: courses_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.courses_id_seq', 12, true);


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

SELECT pg_catalog.setval('zoopick.rooms_id_seq', 6, true);


--
-- Name: timetables_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.timetables_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: zoopick; Owner: postgres
--

SELECT pg_catalog.setval('zoopick.users_id_seq', 5, true);


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
-- Name: timetables timetables_pkey; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT timetables_pkey PRIMARY KEY (id);


--
-- Name: courses uq_course_slot; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.courses
    ADD CONSTRAINT uq_course_slot UNIQUE (room_id, year, semester, day_of_week, start_time);


--
-- Name: cctv_detection_matches uq_detection_matches; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_detection_matches
    ADD CONSTRAINT uq_detection_matches UNIQUE (detection_id, item_id);


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
-- Name: timetables uq_user_course; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT uq_user_course UNIQUE (user_id, course_id);


--
-- Name: users uq_user_email; Type: CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.users
    ADD CONSTRAINT uq_user_email UNIQUE (school_email);


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
-- Name: idx_chatrooms_open; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_chatrooms_open ON zoopick.chat_rooms USING btree (status) WHERE (status = 'OPEN'::zoopick.chat_room_status);


--
-- Name: idx_commands_pending; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_commands_pending ON zoopick.locker_commands USING btree (locker_id, created_at) WHERE (status = 'PENDING'::zoopick.locker_command_status);


--
-- Name: idx_courses_day_time; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_courses_day_time ON zoopick.courses USING btree (day_of_week, start_time);


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
-- Name: idx_matches_score; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_matches_score ON zoopick.item_matches USING btree (score_total DESC);


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
-- Name: idx_timetables_user; Type: INDEX; Schema: zoopick; Owner: postgres
--

CREATE INDEX idx_timetables_user ON zoopick.timetables USING btree (user_id);


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
-- Name: timetables fk_user_courses_course; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT fk_user_courses_course FOREIGN KEY (course_id) REFERENCES zoopick.courses(id) ON DELETE CASCADE;


--
-- Name: timetables fk_user_courses_user; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.timetables
    ADD CONSTRAINT fk_user_courses_user FOREIGN KEY (user_id) REFERENCES zoopick.users(id) ON DELETE CASCADE;


--
-- Name: cctv_videos fk_videos_room; Type: FK CONSTRAINT; Schema: zoopick; Owner: postgres
--

ALTER TABLE ONLY zoopick.cctv_videos
    ADD CONSTRAINT fk_videos_room FOREIGN KEY (room_id) REFERENCES zoopick.rooms(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict zaaZzBLaYPZ6eE7GVrQDofgIBJxSzOfI7E2VSnwefDtdveDFbB7ctnhSUSWhGIv

