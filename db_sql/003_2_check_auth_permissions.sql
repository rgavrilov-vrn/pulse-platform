-- Каталог
SELECT resource, count(*) AS perms
FROM auth.permissions GROUP BY resource ORDER BY resource;

-- Матрица роль × пермишен (сводка)
SELECT r.code AS role, count(rp.permission_id) AS perm_count
FROM auth.roles r
         LEFT JOIN auth.role_permissions rp ON rp.role_id = r.id
GROUP BY r.code ORDER BY r.code;

-- Пользователи и роли
SELECT u.login, string_agg(r.code, ', ' ORDER BY r.code) AS roles
FROM auth.users u
         LEFT JOIN auth.user_roles ur ON ur.user_id = u.id
         LEFT JOIN auth.roles r       ON r.id = ur.role_id
GROUP BY u.login ORDER BY u.login;

-- Пароль admin теперь реально 'admin' (bcrypt cost 12, prefix $2a$):
SELECT login, left(password_hash, 7) AS hash_prefix
FROM auth.users WHERE login = 'admin';

-- Функция-проверка
SELECT auth.user_has_permission(
               (SELECT id FROM auth.users WHERE login = 'reviewer'),
               'incident:review'
       );   -- → true

SELECT auth.user_has_permission(
               (SELECT id FROM auth.users WHERE login = 'viewer'),
               'incident:review'
       );   -- → false

SELECT auth.user_has_permission(
               (SELECT id FROM auth.users WHERE login = '__system__'),
               'source_event:process'
       );   -- → true