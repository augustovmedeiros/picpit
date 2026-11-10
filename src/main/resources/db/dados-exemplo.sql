-- Dados de exemplo para o modo demonstração (mvn -Pdemo jetty:run).
-- Não use ponto e vírgula dentro dos textos (o ScriptSqlRunner separa os comandos por ele).

INSERT INTO item_catalogo (titulo, tipo, genero, ano_lancamento, criador, nota, sinopse) VALUES
('Dom Casmurro', 'LIVRO', 'Romance', 1899, 'Machado de Assis', 9.5,
 'Bentinho relembra sua vida e o ciúme que sentia de Capitu, deixando ao leitor a dúvida sobre a traição.'),
('O Hobbit', 'LIVRO', 'Fantasia', 1937, 'J. R. R. Tolkien', 9.0,
 'Bilbo Bolseiro é arrastado por um mago e treze anões para uma aventura em busca de um tesouro guardado por um dragão.'),
('Grande Sertão: Veredas', 'LIVRO', 'Romance', 1956, 'João Guimarães Rosa', 9.8,
 'O ex-jagunço Riobaldo narra suas andanças pelo sertão e seu amor por Diadorim.'),
('Breaking Bad', 'SERIE', 'Drama', 2008, 'Vince Gilligan', 9.7,
 'Um professor de química com câncer passa a fabricar metanfetamina para garantir o futuro da família.'),
('Dark', 'SERIE', 'Ficção científica', 2017, 'Baran bo Odar e Jantje Friese', 9.2,
 'O desaparecimento de crianças revela segredos de quatro famílias e um ciclo de viagens no tempo.'),
('Cidade de Deus', 'FILME', 'Drama', 2002, 'Fernando Meirelles', 9.4,
 'Buscapé, um jovem fotógrafo, acompanha o crescimento do crime organizado na comunidade onde vive.'),
('O Auto da Compadecida', 'FILME', 'Comédia', 2000, 'Guel Arraes', 9.6,
 'João Grilo e Chicó aplicam golpes no sertão nordestino até enfrentarem o julgamento final.'),
('Interestelar', 'FILME', 'Ficção científica', 2014, 'Christopher Nolan', 8.9, NULL);

