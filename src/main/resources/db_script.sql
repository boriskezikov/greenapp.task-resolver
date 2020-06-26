CREATE TYPE task_status AS ENUM (
    'TRASHED',
    'CREATED',
    'WAITING_FOR_APPROVE',
    'APPROVED',
    'TO_DO',
    'IN_PROGRESS',
    'RESOLVED',
    'COMPLETED'
    );

CREATE TYPE vote_type AS ENUM (
    'APPROVE',
    'REJECT'
    );


CREATE TABLE public.task
(
    task_id BIGINT PRIMARY KEY,
    status  public.task_status    NOT NULL,
    counter BIGINT    DEFAULT (0) NOT NULL,
    created timestamp DEFAULT now(),
    CONSTRAINT unique_task_id_constr UNIQUE (task_id)
);

CREATE TABLE public.client
(
    client_id BIGINT PRIMARY KEY,
    task_id   BIGINT           NOT NULL,
    type      public.vote_type NOT NULL
);

