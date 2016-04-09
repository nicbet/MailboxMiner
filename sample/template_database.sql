--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- Name: ml-template; Type: COMMENT; Schema: -; Owner: nicbet
--

COMMENT ON DATABASE "ml-template" IS 'Template Database for MailboxMiner2. Do not modify this database, nor put any data!';


--
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: nicbet
--

CREATE PROCEDURAL LANGUAGE plpgsql;


ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO nicbet;

SET search_path = public, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: attachments; Type: TABLE; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE TABLE attachments (
    msg_id integer,
    mime_type text,
    data bytea,
    order_id integer,
    size bigint
);


ALTER TABLE public.attachments OWNER TO nicbet;

--
-- Name: TABLE attachments; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON TABLE attachments IS 'Stores all attachments to an electronic message.';


--
-- Name: COLUMN attachments.mime_type; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON COLUMN attachments.mime_type IS 'mime-type if it can be determined';


--
-- Name: COLUMN attachments.data; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON COLUMN attachments.data IS 'Byte Sequence of the data';


--
-- Name: COLUMN attachments.order_id; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON COLUMN attachments.order_id IS 'Order of the attachments is important!';


--
-- Name: COLUMN attachments.size; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON COLUMN attachments.size IS 'size (in bytes) of the attachment';


--
-- Name: bodies; Type: TABLE; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE TABLE bodies (
    msg_id integer,
    body_txt text,
    body_type text,
    body_txt_cleaned text,
    note text
);


ALTER TABLE public.bodies OWNER TO nicbet;

--
-- Name: TABLE bodies; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON TABLE bodies IS 'Stores the email''s textual (main) bodies.';


--
-- Name: COLUMN bodies.note; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON COLUMN bodies.note IS 'Any note specifice to the body text';


--
-- Name: headers; Type: TABLE; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE TABLE headers (
    msg_id integer,
    header_key text,
    header_value text
);


ALTER TABLE public.headers OWNER TO nicbet;

--
-- Name: TABLE headers; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON TABLE headers IS 'Stores the electronic mail headers key-value pairs.';


--
-- Name: messages; Type: TABLE; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE TABLE messages (
    msg_id integer NOT NULL,
    msg_sender_name text,
    msg_sender_address text,
    msg_date timestamp with time zone,
    msg_subject text
);


ALTER TABLE public.messages OWNER TO nicbet;

--
-- Name: TABLE messages; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON TABLE messages IS 'Stores the electronic messages together with the 5 header fields, required for every mail.';


--
-- Name: parent; Type: TABLE; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE TABLE parent (
    msg_id integer NOT NULL,
    parent_id integer
);


ALTER TABLE public.parent OWNER TO nicbet;

--
-- Name: recipients; Type: TABLE; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE TABLE recipients (
    msg_id integer,
    recipient_type text,
    recipient_name text,
    recipient_address text
);


ALTER TABLE public.recipients OWNER TO nicbet;

--
-- Name: TABLE recipients; Type: COMMENT; Schema: public; Owner: nicbet
--

COMMENT ON TABLE recipients IS 'Stores the recipients of electronic messages.';


--
-- Name: root; Type: TABLE; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE TABLE root (
    msg_id integer NOT NULL,
    root_id integer
);


ALTER TABLE public.root OWNER TO nicbet;

--
-- Name: array_accum(anyelement); Type: AGGREGATE; Schema: public; Owner: nicbet
--

CREATE AGGREGATE array_accum(anyelement) (
    SFUNC = array_append,
    STYPE = anyarray,
    INITCOND = '{}'
);


ALTER AGGREGATE public.array_accum(anyelement) OWNER TO nicbet;

--
-- Name: view_headers; Type: VIEW; Schema: public; Owner: nicbet
--

CREATE VIEW view_headers AS
    SELECT data.msg_id, array_accum(data.kv) AS headers FROM (SELECT headers.msg_id, ((headers.header_key || ': '::text) || headers.header_value) AS kv FROM headers GROUP BY headers.msg_id, headers.header_key, headers.header_value) data GROUP BY data.msg_id;


ALTER TABLE public.view_headers OWNER TO nicbet;

--
-- Name: merge_parent(integer, integer); Type: FUNCTION; Schema: public; Owner: nicbet
--

CREATE FUNCTION merge_parent(key integer, parentid integer) RETURNS void
    AS $$ 
BEGIN 
	LOOP 
		BEGIN 
			INSERT INTO parent(msg_id, parent_id) VALUES (key, parentid); 
			RETURN; 
		EXCEPTION WHEN unique_violation THEN 
			RETURN;
			-- do nothing 
		END; 
	END LOOP; 
END; 
$$
    LANGUAGE plpgsql;


ALTER FUNCTION public.merge_parent(key integer, parentid integer) OWNER TO nicbet;

--
-- Name: merge_root(integer, integer); Type: FUNCTION; Schema: public; Owner: nicbet
--

CREATE FUNCTION merge_root(key integer, rootid integer) RETURNS void
    AS $$ 
BEGIN 
	LOOP 
		UPDATE root SET root_id = rootid WHERE msg_id = key; 
		IF found THEN 
			RETURN; 
		END IF;

		BEGIN 
			INSERT INTO root(msg_id, root_id) VALUES (key, rootid); 
			RETURN; 
		EXCEPTION WHEN unique_violation THEN 
			-- do nothing 
		END; 
	END LOOP; 
END; 
$$
    LANGUAGE plpgsql;


ALTER FUNCTION public.merge_root(key integer, rootid integer) OWNER TO nicbet;

--
-- Name: messages_msg_id_seq; Type: SEQUENCE; Schema: public; Owner: nicbet
--

CREATE SEQUENCE messages_msg_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.messages_msg_id_seq OWNER TO nicbet;

--
-- Name: messages_msg_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: nicbet
--

ALTER SEQUENCE messages_msg_id_seq OWNED BY messages.msg_id;


--
-- Name: msg_id; Type: DEFAULT; Schema: public; Owner: nicbet
--

ALTER TABLE messages ALTER COLUMN msg_id SET DEFAULT nextval('messages_msg_id_seq'::regclass);


--
-- Name: messages_pkey; Type: CONSTRAINT; Schema: public; Owner: nicbet; Tablespace: 
--

ALTER TABLE ONLY messages
    ADD CONSTRAINT messages_pkey PRIMARY KEY (msg_id);


--
-- Name: msg_id_pkey; Type: CONSTRAINT; Schema: public; Owner: nicbet; Tablespace: 
--

ALTER TABLE ONLY parent
    ADD CONSTRAINT msg_id_pkey PRIMARY KEY (msg_id);


--
-- Name: msg_id_pkey2; Type: CONSTRAINT; Schema: public; Owner: nicbet; Tablespace: 
--

ALTER TABLE ONLY root
    ADD CONSTRAINT msg_id_pkey2 PRIMARY KEY (msg_id);


--
-- Name: index_attachments; Type: INDEX; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE INDEX index_attachments ON attachments USING btree (msg_id, mime_type);


--
-- Name: index_bodies; Type: INDEX; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE INDEX index_bodies ON bodies USING btree (msg_id, body_type, note);


--
-- Name: index_headers; Type: INDEX; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE INDEX index_headers ON headers USING btree (msg_id, header_key);


--
-- Name: index_messages; Type: INDEX; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE INDEX index_messages ON messages USING btree (msg_id, msg_sender_name, msg_sender_address, msg_date, msg_subject);


--
-- Name: index_parent; Type: INDEX; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE INDEX index_parent ON parent USING btree (msg_id, parent_id);


--
-- Name: index_recipients; Type: INDEX; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE INDEX index_recipients ON recipients USING btree (msg_id, recipient_type);


--
-- Name: index_root; Type: INDEX; Schema: public; Owner: nicbet; Tablespace: 
--

CREATE INDEX index_root ON root USING btree (msg_id, root_id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;
GRANT ALL ON SCHEMA public TO mailinglists;


--
-- Name: attachments; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE attachments FROM PUBLIC;
REVOKE ALL ON TABLE attachments FROM nicbet;
GRANT ALL ON TABLE attachments TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE attachments TO PUBLIC;
GRANT ALL ON TABLE attachments TO mailinglists;


--
-- Name: bodies; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE bodies FROM PUBLIC;
REVOKE ALL ON TABLE bodies FROM nicbet;
GRANT ALL ON TABLE bodies TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE bodies TO PUBLIC;
GRANT ALL ON TABLE bodies TO mailinglists;


--
-- Name: headers; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE headers FROM PUBLIC;
REVOKE ALL ON TABLE headers FROM nicbet;
GRANT ALL ON TABLE headers TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE headers TO PUBLIC;
GRANT ALL ON TABLE headers TO mailinglists;


--
-- Name: messages; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE messages FROM PUBLIC;
REVOKE ALL ON TABLE messages FROM nicbet;
GRANT ALL ON TABLE messages TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE messages TO PUBLIC;
GRANT ALL ON TABLE messages TO mailinglists;


--
-- Name: parent; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE parent FROM PUBLIC;
REVOKE ALL ON TABLE parent FROM nicbet;
GRANT ALL ON TABLE parent TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE parent TO PUBLIC;
GRANT ALL ON TABLE parent TO mailinglists;


--
-- Name: recipients; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE recipients FROM PUBLIC;
REVOKE ALL ON TABLE recipients FROM nicbet;
GRANT ALL ON TABLE recipients TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE recipients TO PUBLIC;
GRANT ALL ON TABLE recipients TO mailinglists;


--
-- Name: root; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE root FROM PUBLIC;
REVOKE ALL ON TABLE root FROM nicbet;
GRANT ALL ON TABLE root TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE root TO PUBLIC;
GRANT ALL ON TABLE root TO mailinglists;


--
-- Name: view_headers; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON TABLE view_headers FROM PUBLIC;
REVOKE ALL ON TABLE view_headers FROM nicbet;
GRANT ALL ON TABLE view_headers TO nicbet;
GRANT SELECT,INSERT,REFERENCES,TRIGGER ON TABLE view_headers TO PUBLIC;
GRANT ALL ON TABLE view_headers TO mailinglists;


--
-- Name: merge_parent(integer, integer); Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON FUNCTION merge_parent(key integer, parentid integer) FROM PUBLIC;
REVOKE ALL ON FUNCTION merge_parent(key integer, parentid integer) FROM nicbet;
GRANT ALL ON FUNCTION merge_parent(key integer, parentid integer) TO nicbet;
GRANT ALL ON FUNCTION merge_parent(key integer, parentid integer) TO PUBLIC;
GRANT ALL ON FUNCTION merge_parent(key integer, parentid integer) TO mailinglists;


--
-- Name: merge_root(integer, integer); Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON FUNCTION merge_root(key integer, rootid integer) FROM PUBLIC;
REVOKE ALL ON FUNCTION merge_root(key integer, rootid integer) FROM nicbet;
GRANT ALL ON FUNCTION merge_root(key integer, rootid integer) TO nicbet;
GRANT ALL ON FUNCTION merge_root(key integer, rootid integer) TO PUBLIC;
GRANT ALL ON FUNCTION merge_root(key integer, rootid integer) TO mailinglists;


--
-- Name: messages_msg_id_seq; Type: ACL; Schema: public; Owner: nicbet
--

REVOKE ALL ON SEQUENCE messages_msg_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE messages_msg_id_seq FROM nicbet;
GRANT ALL ON SEQUENCE messages_msg_id_seq TO nicbet;
GRANT SELECT ON SEQUENCE messages_msg_id_seq TO PUBLIC;
GRANT ALL ON SEQUENCE messages_msg_id_seq TO mailinglists;


--
-- PostgreSQL database dump complete
--

