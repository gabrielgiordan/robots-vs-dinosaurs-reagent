FROM nginx:alpine

COPY nginx.conf /etc/nginx/nginx.conf
COPY ./public /etc/nginx/html

CMD sed -i -e 's/$PORT/'"$PORT"'/g' /etc/nginx/nginx.conf && nginx -g 'daemon off;'
