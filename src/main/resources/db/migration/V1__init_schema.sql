CREATE TABLE category (
	id INT PRIMARY KEY IDENTITY(1, 1),
	name NVARCHAR(30) UNIQUE NOT NULL,
	color CHAR(7) NOT NULL
);

CREATE TABLE transfer (
	id INT PRIMARY KEY IDENTITY(1, 1),
	type VARCHAR(7) NOT NULL,
	amount DECIMAL(9, 2) NOT NULL,
	date DATE NOT NULL,
	description NVARCHAR(200),
	category_id INT NOT NULL
);

CREATE TABLE budget (
	month DATE NOT NULL,
	category_id INT NOT NULL,
	amount DECIMAL(9, 2) NOT NULL
);

ALTER TABLE category
ADD
	CONSTRAINT ck_category_name CHECK (
		name = LTRIM(RTRIM(name)) AND
		LEN(name) >= 1
	),
	CONSTRAINT ck_category_color CHECK (
		color = LTRIM(RTRIM(color)) AND
		LEN(color) = 7 AND
		color LIKE '#[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f]'
	);

ALTER TABLE transfer
ADD
	CONSTRAINT ck_transfer_type CHECK ( type IN ('INCOME', 'EXPENSE') ),
	CONSTRAINT ck_transfer_amount CHECK ( amount > 0 AND amount <= 1000000.00 ),
	CONSTRAINT fk_transfer_category
		FOREIGN KEY (category_id) REFERENCES category(id)
		ON UPDATE CASCADE ON DELETE NO ACTION;

ALTER TABLE budget
ADD
	CONSTRAINT ck_budget_month CHECK (DAY(month) = 1),
	CONSTRAINT fk_budget_category
		FOREIGN KEY (category_id) REFERENCES category(id)
		ON UPDATE CASCADE ON DELETE CASCADE,
	CONSTRAINT ck_budget_amount CHECK ( amount > 0 AND amount <= 1000000.00 ),
	CONSTRAINT pk_budget_category_month PRIMARY KEY (category_id, month);
